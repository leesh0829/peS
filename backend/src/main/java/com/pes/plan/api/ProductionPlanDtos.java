package com.pes.plan.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.pes.plan.domain.ProductionPlan;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductUnit;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class ProductionPlanDtos {

	private ProductionPlanDtos() {
	}

	public record CreateRequest(
			@NotNull UUID productId,
			@NotNull @FutureOrPresent LocalDate dueDate,
			@Min(1) int targetQuantity) {
	}

	public record ProductSummary(UUID id, String code, String name, ProductUnit unit) {

		public static ProductSummary from(Product product) {
			return new ProductSummary(product.getId(), product.getCode(), product.getName(), product.getUnit());
		}
	}

	public record Response(
			UUID id,
			String planNumber,
			ProductSummary product,
			LocalDate dueDate,
			int targetQuantity,
			ProductionPlanStatus status,
			long allocatedQuantity,
			long remainingQuantity,
			long workOrderCount,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		public static Response from(ProductionPlan plan, long allocatedQuantity, long workOrderCount) {
			return new Response(
					plan.getId(),
					plan.getPlanNumber(),
					ProductSummary.from(plan.getProduct()),
					plan.getDueDate(),
					plan.getTargetQuantity(),
					plan.getStatus(),
					allocatedQuantity,
					plan.getTargetQuantity() - allocatedQuantity,
					workOrderCount,
					plan.getVersion(),
					plan.getCreatedAt(),
					plan.getUpdatedAt());
		}
	}
}
