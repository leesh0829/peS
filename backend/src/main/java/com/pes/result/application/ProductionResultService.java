package com.pes.result.application;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.ForbiddenException;
import com.pes.common.error.InvalidRequestException;
import com.pes.common.error.NotFoundException;
import com.pes.result.api.ProductionResultDtos;
import com.pes.result.domain.ProductionResult;
import com.pes.result.domain.ProductionResultRepository;
import com.pes.user.domain.UserRole;
import com.pes.workorder.domain.WorkOrder;
import com.pes.workorder.domain.WorkOrderRepository;
import com.pes.workorder.domain.WorkOrderStatus;

@Service
@Transactional(readOnly = true)
public class ProductionResultService {

	private final ProductionResultRepository repository;
	private final WorkOrderRepository workOrderRepository;

	public ProductionResultService(
			ProductionResultRepository repository,
			WorkOrderRepository workOrderRepository) {
		this.repository = repository;
		this.workOrderRepository = workOrderRepository;
	}

	public PageResponse<ProductionResultDtos.Response> search(
			String search, int page, int size, PesUserPrincipal principal) {
		String normalizedSearch = search == null ? "" : search.trim();
		PageRequest pageable = PageRequest.of(
				page,
				Math.min(size, 100),
				Sort.by(Sort.Order.desc("createdAt")));
		Page<ProductionResult> results = principal.role() == UserRole.WORKER
				? repository.searchAssigned(principal.id(), normalizedSearch, pageable)
				: repository.searchAll(normalizedSearch, pageable);
		return PageResponse.from(results, ProductionResultDtos.Response::from);
	}

	@Transactional
	public ProductionResultDtos.Response create(
			UUID workOrderId,
			ProductionResultDtos.CreateRequest request,
			PesUserPrincipal principal) {
		validateQuantities(request);
		WorkOrder workOrder = workOrderRepository.findByIdForUpdate(workOrderId)
				.orElseThrow(() -> new NotFoundException("작업지시를 찾을 수 없습니다."));
		if (!workOrder.getAssignedWorker().getId().equals(principal.id())) {
			throw new ForbiddenException("배정된 작업자만 생산실적을 등록할 수 있습니다.");
		}
		if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
			throw new ConflictException("작업 중인 작업지시에만 생산실적을 등록할 수 있습니다.");
		}

		long accumulatedQuantity = repository.sumProducedQuantityByWorkOrderId(workOrderId);
		if (accumulatedQuantity + request.producedQuantity() > workOrder.getTargetQuantity()) {
			throw new ConflictException("누적 생산수량은 작업지시 목표수량을 초과할 수 없습니다.");
		}

		ProductionResult result = new ProductionResult(
				workOrder,
				request.producedQuantity(),
				request.goodQuantity(),
				request.defectQuantity(),
				workOrder.getAssignedWorker());
		return ProductionResultDtos.Response.from(repository.saveAndFlush(result));
	}

	private void validateQuantities(ProductionResultDtos.CreateRequest request) {
		if (request.producedQuantity() <= 0 || request.goodQuantity() < 0 || request.defectQuantity() < 0) {
			throw new InvalidRequestException("생산수량은 1 이상이고 양품·불량수량은 0 이상이어야 합니다.");
		}
		if (request.producedQuantity() != request.goodQuantity() + request.defectQuantity()) {
			throw new InvalidRequestException("생산수량은 양품수량과 불량수량의 합과 같아야 합니다.");
		}
	}
}
