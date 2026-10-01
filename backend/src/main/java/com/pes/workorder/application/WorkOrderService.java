package com.pes.workorder.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.ForbiddenException;
import com.pes.common.error.NotFoundException;
import com.pes.plan.domain.ProductionPlan;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.process.domain.ProductionProcess;
import com.pes.process.domain.ProductionProcessRepository;
import com.pes.result.domain.ProductionResultRepository;
import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;
import com.pes.workorder.api.WorkOrderDtos;
import com.pes.workorder.domain.WorkOrder;
import com.pes.workorder.domain.WorkOrderRepository;
import com.pes.workorder.domain.WorkOrderStatus;
import com.pes.lot.domain.WorkOrderMaterialRepository;
import com.pes.product.domain.ProductUnit;

@Service
@Transactional(readOnly = true)
public class WorkOrderService {

	private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final WorkOrderRepository repository;
	private final ProductionPlanRepository productionPlanRepository;
	private final ProductionProcessRepository processRepository;
	private final UserAccountRepository userRepository;
	private final ProductionResultRepository productionResultRepository;
	private final WorkOrderMaterialRepository materialRepository;

	public WorkOrderService(
			WorkOrderRepository repository,
			ProductionPlanRepository productionPlanRepository,
			ProductionProcessRepository processRepository,
			UserAccountRepository userRepository,
			ProductionResultRepository productionResultRepository,
			WorkOrderMaterialRepository materialRepository) {
		this.repository = repository;
		this.productionPlanRepository = productionPlanRepository;
		this.processRepository = processRepository;
		this.userRepository = userRepository;
		this.productionResultRepository = productionResultRepository;
		this.materialRepository = materialRepository;
	}

	public PageResponse<WorkOrderDtos.Response> search(
			String search, WorkOrderStatus status, int page, int size, PesUserPrincipal principal) {
		String normalizedSearch = search == null ? "" : search.trim();
		PageRequest pageable = PageRequest.of(
				page,
				Math.min(size, 100),
				Sort.by(Sort.Order.desc("createdAt")));
		Page<WorkOrder> workOrders = principal.role() == UserRole.WORKER
				? repository.searchAssigned(principal.id(), normalizedSearch, status, pageable)
				: repository.searchAll(normalizedSearch, status, pageable);
		Map<UUID, ProductionResultRepository.WorkOrderQuantitySummary> summaries = workOrders.isEmpty()
				? Map.of()
				: productionResultRepository
						.summarizeByWorkOrderIdIn(workOrders.getContent().stream().map(WorkOrder::getId).toList())
						.stream()
						.collect(Collectors.toMap(
								ProductionResultRepository.WorkOrderQuantitySummary::getWorkOrderId,
								Function.identity()));
		return PageResponse.from(workOrders, workOrder -> toResponse(workOrder, summaries.get(workOrder.getId())));
	}

	@Transactional
	public WorkOrderDtos.Response create(WorkOrderDtos.CreateRequest request) {
		ProductionPlan plan = productionPlanRepository.findByIdForUpdate(request.productionPlanId())
				.orElseThrow(() -> new NotFoundException("생산계획을 찾을 수 없습니다."));
		if (plan.getStatus() != ProductionPlanStatus.CONFIRMED) {
			throw new ConflictException("확정된 생산계획에만 작업지시를 등록할 수 있습니다.");
		}

		ProductionProcess process = processRepository.findById(request.productionProcessId())
				.orElseThrow(() -> new NotFoundException("공정을 찾을 수 없습니다."));
		if (!process.isActive()) {
			throw new ConflictException("사용 중인 공정만 작업지시에 지정할 수 있습니다.");
		}

		UserAccount worker = userRepository.findById(request.assignedWorkerId())
				.orElseThrow(() -> new NotFoundException("작업자를 찾을 수 없습니다."));
		if (!worker.isActive() || worker.getRole() != UserRole.WORKER) {
			throw new ConflictException("활성 작업자 계정만 작업지시에 배정할 수 있습니다.");
		}

		long allocatedQuantity = repository.sumTargetQuantityByProductionPlanId(plan.getId());
		if (allocatedQuantity + request.targetQuantity() > plan.getTargetQuantity()) {
			throw new ConflictException("작업지시 수량 합계는 생산계획 목표수량을 초과할 수 없습니다.");
		}

		WorkOrder workOrder = new WorkOrder(
				generateWorkOrderNumber(), plan, process, worker, request.targetQuantity());
		if (request.lotTrackingEnabled()) {
			if (plan.getProduct().getUnit() != ProductUnit.EACH) {
				throw new ConflictException("LOT 추적은 개수(EACH) 단위 품목만 지원합니다.");
			}
			workOrder.enableLotTracking();
		}
		return WorkOrderDtos.Response.from(repository.saveAndFlush(workOrder), 0, 0, 0);
	}

	@Transactional
	public WorkOrderDtos.Response start(UUID id, PesUserPrincipal principal) {
		WorkOrder workOrder = repository.findByIdForUpdate(id)
				.orElseThrow(() -> new NotFoundException("작업지시를 찾을 수 없습니다."));
		if (!workOrder.getAssignedWorker().getId().equals(principal.id())) {
			throw new ForbiddenException("배정된 작업자만 작업을 시작할 수 있습니다.");
		}
		if (workOrder.isLotTrackingEnabled() && materialRepository.sumByWorkOrderId(id) != workOrder.getTargetQuantity()) {
			throw new ConflictException("자재 투입수량이 목표수량과 같아야 작업을 시작할 수 있습니다.");
		}
		workOrder.start(Instant.now());
		repository.flush();
		return toResponse(workOrder, productionResultRepository.summarizeByWorkOrderId(id).orElse(null));
	}

	@Transactional
	public WorkOrderDtos.Response complete(UUID id, PesUserPrincipal principal) {
		WorkOrder workOrder = repository.findByIdForUpdate(id)
				.orElseThrow(() -> new NotFoundException("작업지시를 찾을 수 없습니다."));
		if (!workOrder.getAssignedWorker().getId().equals(principal.id())) {
			throw new ForbiddenException("배정된 작업자만 작업을 완료할 수 있습니다.");
		}
		if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
			throw new ConflictException("작업 중인 작업지시만 완료할 수 있습니다.");
		}
		ProductionResultRepository.WorkOrderQuantitySummary summary = productionResultRepository
				.summarizeByWorkOrderId(id)
				.orElse(null);
		long producedQuantity = summary == null ? 0 : summary.getProducedQuantity();
		if (producedQuantity != workOrder.getTargetQuantity()) {
			throw new ConflictException("누적 생산수량이 목표수량과 같을 때만 작업을 완료할 수 있습니다.");
		}
		workOrder.complete(Instant.now());
		repository.flush();
		return toResponse(workOrder, summary);
	}

	private WorkOrderDtos.Response toResponse(
			WorkOrder workOrder,
			ProductionResultRepository.WorkOrderQuantitySummary summary) {
		return WorkOrderDtos.Response.from(
				workOrder,
				summary == null ? 0 : summary.getProducedQuantity(),
				summary == null ? 0 : summary.getGoodQuantity(),
				summary == null ? 0 : summary.getDefectQuantity());
	}

	private String generateWorkOrderNumber() {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		return "WO-" + LocalDate.now().format(NUMBER_DATE) + "-" + suffix;
	}
}
