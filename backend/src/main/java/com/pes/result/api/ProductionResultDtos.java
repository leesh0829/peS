package com.pes.result.api;

import java.time.Instant;
import java.util.UUID;

import com.pes.result.domain.ProductionResult;

import jakarta.validation.constraints.Min;

public final class ProductionResultDtos {

	private ProductionResultDtos() {
	}

	public record CreateRequest(
			@Min(1) int producedQuantity,
			@Min(0) int goodQuantity,
			@Min(0) int defectQuantity) {
	}

	public record WorkOrderSummary(UUID id, String workOrderNumber, int targetQuantity) {
	}

	public record ProductSummary(UUID id, String code, String name) {
	}

	public record RecorderSummary(UUID id, String username, String displayName) {
	}

	public record Response(
			UUID id,
			WorkOrderSummary workOrder,
			ProductSummary product,
			int producedQuantity,
			int goodQuantity,
			int defectQuantity,
			RecorderSummary recordedBy,
			Instant recordedAt) {

		public static Response from(ProductionResult result) {
			var workOrder = result.getWorkOrder();
			var product = workOrder.getProductionPlan().getProduct();
			var recorder = result.getRecordedBy();
			return new Response(
					result.getId(),
					new WorkOrderSummary(workOrder.getId(), workOrder.getWorkOrderNumber(), workOrder.getTargetQuantity()),
					new ProductSummary(product.getId(), product.getCode(), product.getName()),
					result.getProducedQuantity(),
					result.getGoodQuantity(),
					result.getDefectQuantity(),
					new RecorderSummary(recorder.getId(), recorder.getUsername(), recorder.getDisplayName()),
					result.getCreatedAt());
		}
	}
}
