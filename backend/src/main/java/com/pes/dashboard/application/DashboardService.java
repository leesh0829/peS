package com.pes.dashboard.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.dashboard.api.DashboardDtos;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.result.domain.ProductionResultRepository;
import com.pes.workorder.domain.WorkOrderRepository;
import com.pes.workorder.domain.WorkOrderStatus;

@Service
@Transactional(readOnly = true)
public class DashboardService {

	private final ProductionPlanRepository productionPlanRepository;
	private final WorkOrderRepository workOrderRepository;
	private final ProductionResultRepository productionResultRepository;

	public DashboardService(
			ProductionPlanRepository productionPlanRepository,
			WorkOrderRepository workOrderRepository,
			ProductionResultRepository productionResultRepository) {
		this.productionPlanRepository = productionPlanRepository;
		this.workOrderRepository = workOrderRepository;
		this.productionResultRepository = productionResultRepository;
	}

	public DashboardDtos.Response getSummary() {
		long confirmedPlanCount = productionPlanRepository.countByStatus(ProductionPlanStatus.CONFIRMED);
		long plannedQuantity = productionPlanRepository.sumTargetQuantityByStatus(ProductionPlanStatus.CONFIRMED);
		long orderedQuantity = workOrderRepository.sumTargetQuantity();
		long waitingCount = workOrderRepository.countByStatus(WorkOrderStatus.WAITING);
		long inProgressCount = workOrderRepository.countByStatus(WorkOrderStatus.IN_PROGRESS);
		long completedCount = workOrderRepository.countByStatus(WorkOrderStatus.COMPLETED);
		ProductionResultRepository.QuantitySummary resultSummary = productionResultRepository.summarizeAll();

		return new DashboardDtos.Response(
				confirmedPlanCount,
				new DashboardDtos.WorkOrderStatusSummary(
						waitingCount,
						inProgressCount,
						completedCount,
						waitingCount + inProgressCount + completedCount),
				new DashboardDtos.QuantitySummary(
						plannedQuantity,
						orderedQuantity,
						resultSummary.getProducedQuantity(),
						resultSummary.getGoodQuantity(),
						resultSummary.getDefectQuantity()),
				percentage(resultSummary.getProducedQuantity(), plannedQuantity),
				percentage(resultSummary.getGoodQuantity(), resultSummary.getProducedQuantity()));
	}

	private Double percentage(long numerator, long denominator) {
		if (denominator == 0) {
			return null;
		}
		return Math.round(numerator * 1000.0 / denominator) / 10.0;
	}
}
