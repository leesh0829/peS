package com.pes.workorder.domain;

import java.time.Instant;

import com.pes.common.error.ConflictException;
import com.pes.common.persistence.BaseEntity;
import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.user.domain.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {

	@Column(name = "work_order_number", nullable = false, unique = true, length = 30)
	private String workOrderNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "production_plan_id", nullable = false)
	private ProductionPlan productionPlan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "production_process_id", nullable = false)
	private ProductionProcess productionProcess;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assigned_worker_id", nullable = false)
	private UserAccount assignedWorker;

	@Column(name = "target_quantity", nullable = false)
	private int targetQuantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private WorkOrderStatus status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	protected WorkOrder() {
	}

	public WorkOrder(
			String workOrderNumber,
			ProductionPlan productionPlan,
			ProductionProcess productionProcess,
			UserAccount assignedWorker,
			int targetQuantity) {
		this.workOrderNumber = workOrderNumber;
		this.productionPlan = productionPlan;
		this.productionProcess = productionProcess;
		this.assignedWorker = assignedWorker;
		this.targetQuantity = targetQuantity;
		this.status = WorkOrderStatus.WAITING;
	}

	public void start(Instant startedAt) {
		if (status != WorkOrderStatus.WAITING) {
			throw new ConflictException("대기 상태의 작업지시만 시작할 수 있습니다.");
		}
		status = WorkOrderStatus.IN_PROGRESS;
		this.startedAt = startedAt;
	}

	public String getWorkOrderNumber() {
		return workOrderNumber;
	}

	public ProductionPlan getProductionPlan() {
		return productionPlan;
	}

	public ProductionProcess getProductionProcess() {
		return productionProcess;
	}

	public UserAccount getAssignedWorker() {
		return assignedWorker;
	}

	public int getTargetQuantity() {
		return targetQuantity;
	}

	public WorkOrderStatus getStatus() {
		return status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}
}
