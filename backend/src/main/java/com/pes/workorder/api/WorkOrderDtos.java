package com.pes.workorder.api;

import java.time.Instant;
import java.util.UUID;

import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.product.domain.Product;
import com.pes.user.domain.UserAccount;
import com.pes.workorder.domain.WorkOrder;
import com.pes.workorder.domain.WorkOrderStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class WorkOrderDtos {

	private WorkOrderDtos() {
	}

	public record CreateRequest(
			@NotNull UUID productionPlanId,
			@NotNull UUID productionProcessId,
			@NotNull UUID assignedWorkerId,
			@Min(1) int targetQuantity,
			boolean lotTrackingEnabled) {
		public CreateRequest(UUID productionPlanId, UUID productionProcessId, UUID assignedWorkerId, int targetQuantity) {
			this(productionPlanId, productionProcessId, assignedWorkerId, targetQuantity, false);
		}
	}

	public record PlanSummary(UUID id, String planNumber) {
		public static PlanSummary from(ProductionPlan plan) {
			return new PlanSummary(plan.getId(), plan.getPlanNumber());
		}
	}

	public record ProductSummary(UUID id, String code, String name) {
		public static ProductSummary from(Product product) {
			return new ProductSummary(product.getId(), product.getCode(), product.getName());
		}
	}

	public record ProcessSummary(UUID id, String code, String name) {
		public static ProcessSummary from(ProductionProcess process) {
			return new ProcessSummary(process.getId(), process.getCode(), process.getName());
		}
	}

	public record WorkerSummary(UUID id, String username, String displayName) {
		public static WorkerSummary from(UserAccount worker) {
			return new WorkerSummary(worker.getId(), worker.getUsername(), worker.getDisplayName());
		}
	}

	public record Response(
			UUID id,
			String workOrderNumber,
			PlanSummary productionPlan,
			ProductSummary product,
			ProcessSummary productionProcess,
			WorkerSummary assignedWorker,
			int targetQuantity,
			boolean lotTrackingEnabled,
			long producedQuantity,
			long goodQuantity,
			long defectQuantity,
			long remainingQuantity,
			WorkOrderStatus status,
			Instant startedAt,
			Instant completedAt,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		public static Response from(
				WorkOrder workOrder,
				long producedQuantity,
				long goodQuantity,
				long defectQuantity) {
			ProductionPlan plan = workOrder.getProductionPlan();
			return new Response(
					workOrder.getId(),
					workOrder.getWorkOrderNumber(),
					PlanSummary.from(plan),
					ProductSummary.from(plan.getProduct()),
					ProcessSummary.from(workOrder.getProductionProcess()),
					WorkerSummary.from(workOrder.getAssignedWorker()),
					workOrder.getTargetQuantity(),
					workOrder.isLotTrackingEnabled(),
					producedQuantity,
					goodQuantity,
					defectQuantity,
					workOrder.getTargetQuantity() - producedQuantity,
					workOrder.getStatus(),
					workOrder.getStartedAt(),
					workOrder.getCompletedAt(),
					workOrder.getVersion(),
					workOrder.getCreatedAt(),
					workOrder.getUpdatedAt());
		}
	}
}
