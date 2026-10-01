package com.pes.result.domain;

import com.pes.common.persistence.BaseEntity;
import com.pes.user.domain.UserAccount;
import com.pes.workorder.domain.WorkOrder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_result")
public class ProductionResult extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id", nullable = false)
	private WorkOrder workOrder;

	@Column(name = "produced_quantity", nullable = false)
	private int producedQuantity;

	@Column(name = "good_quantity", nullable = false)
	private int goodQuantity;

	@Column(name = "defect_quantity", nullable = false)
	private int defectQuantity;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recorded_by_id", nullable = false)
	private UserAccount recordedBy;

	protected ProductionResult() {
	}

	public ProductionResult(
			WorkOrder workOrder,
			int producedQuantity,
			int goodQuantity,
			int defectQuantity,
			UserAccount recordedBy) {
		this.workOrder = workOrder;
		this.producedQuantity = producedQuantity;
		this.goodQuantity = goodQuantity;
		this.defectQuantity = defectQuantity;
		this.recordedBy = recordedBy;
	}

	public WorkOrder getWorkOrder() {
		return workOrder;
	}

	public int getProducedQuantity() {
		return producedQuantity;
	}

	public int getGoodQuantity() {
		return goodQuantity;
	}

	public int getDefectQuantity() {
		return defectQuantity;
	}

	public UserAccount getRecordedBy() {
		return recordedBy;
	}
}
