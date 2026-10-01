package com.pes.dashboard.api;

public final class DashboardDtos {

	private DashboardDtos() {
	}

	public record WorkOrderStatusSummary(
			long waiting,
			long inProgress,
			long completed,
			long total) {
	}

	public record QuantitySummary(
			long planned,
			long ordered,
			long produced,
			long good,
			long defect) {
	}

	public record Response(
			long confirmedPlanCount,
			WorkOrderStatusSummary workOrders,
			QuantitySummary quantities,
			Double planAchievementRate,
			Double goodRate) {
	}
}
