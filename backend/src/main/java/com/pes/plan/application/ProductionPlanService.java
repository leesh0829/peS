package com.pes.plan.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.NotFoundException;
import com.pes.plan.api.ProductionPlanDtos;
import com.pes.plan.domain.ProductionPlan;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductRepository;
import com.pes.workorder.domain.WorkOrderRepository;

@Service
@Transactional(readOnly = true)
public class ProductionPlanService {

	private static final DateTimeFormatter NUMBER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final ProductionPlanRepository repository;
	private final ProductRepository productRepository;
	private final WorkOrderRepository workOrderRepository;

	public ProductionPlanService(
			ProductionPlanRepository repository,
			ProductRepository productRepository,
			WorkOrderRepository workOrderRepository) {
		this.repository = repository;
		this.productRepository = productRepository;
		this.workOrderRepository = workOrderRepository;
	}

	public PageResponse<ProductionPlanDtos.Response> search(
			String search, ProductionPlanStatus status, int page, int size) {
		String normalizedSearch = search == null ? "" : search.trim();
		PageRequest pageable = PageRequest.of(
				page,
				Math.min(size, 100),
				Sort.by(Sort.Order.desc("dueDate"), Sort.Order.desc("createdAt")));
		Page<ProductionPlan> plans = repository.search(normalizedSearch, status, pageable);
		Map<UUID, WorkOrderRepository.PlanAllocation> allocations = plans.isEmpty()
				? Map.of()
				: workOrderRepository
						.summarizeByProductionPlanIdIn(plans.getContent().stream().map(ProductionPlan::getId).toList())
						.stream()
						.collect(Collectors.toMap(WorkOrderRepository.PlanAllocation::getPlanId, Function.identity()));

		return PageResponse.from(plans, plan -> {
			WorkOrderRepository.PlanAllocation allocation = allocations.get(plan.getId());
			long quantity = allocation == null ? 0 : allocation.getAllocatedQuantity();
			long count = allocation == null ? 0 : allocation.getWorkOrderCount();
			return ProductionPlanDtos.Response.from(plan, quantity, count);
		});
	}

	@Transactional
	public ProductionPlanDtos.Response create(ProductionPlanDtos.CreateRequest request) {
		Product product = productRepository.findById(request.productId())
				.orElseThrow(() -> new NotFoundException("품목을 찾을 수 없습니다."));
		if (!product.isActive()) {
			throw new ConflictException("사용 중인 품목만 생산계획에 지정할 수 있습니다.");
		}
		ProductionPlan plan = new ProductionPlan(
				generatePlanNumber(), product, request.dueDate(), request.targetQuantity());
		return ProductionPlanDtos.Response.from(repository.saveAndFlush(plan), 0, 0);
	}

	@Transactional
	public ProductionPlanDtos.Response confirm(UUID id) {
		ProductionPlan plan = repository.findByIdForUpdate(id)
				.orElseThrow(() -> new NotFoundException("생산계획을 찾을 수 없습니다."));
		plan.confirm();
		repository.flush();
		WorkOrderRepository.PlanAllocation allocation = workOrderRepository.summarizeByProductionPlanId(id)
				.orElse(null);
		long quantity = allocation == null ? 0 : allocation.getAllocatedQuantity();
		long count = allocation == null ? 0 : allocation.getWorkOrderCount();
		return ProductionPlanDtos.Response.from(plan, quantity, count);
	}

	private String generatePlanNumber() {
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		return "PP-" + LocalDate.now().format(NUMBER_DATE) + "-" + suffix;
	}
}
