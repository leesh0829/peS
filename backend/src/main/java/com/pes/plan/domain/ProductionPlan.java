package com.pes.plan.domain;

import java.time.LocalDate;

import com.pes.common.error.ConflictException;
import com.pes.common.persistence.BaseEntity;
import com.pes.product.domain.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_plan")
public class ProductionPlan extends BaseEntity {

	@Column(name = "plan_number", nullable = false, unique = true, length = 30)
	private String planNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "target_quantity", nullable = false)
	private int targetQuantity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductionPlanStatus status;

	protected ProductionPlan() {
	}

	public ProductionPlan(String planNumber, Product product, LocalDate dueDate, int targetQuantity) {
		this.planNumber = planNumber;
		this.product = product;
		this.dueDate = dueDate;
		this.targetQuantity = targetQuantity;
		this.status = ProductionPlanStatus.DRAFT;
	}

	public void confirm() {
		if (status != ProductionPlanStatus.DRAFT) {
			throw new ConflictException("대기 상태의 생산계획만 확정할 수 있습니다.");
		}
		status = ProductionPlanStatus.CONFIRMED;
	}

	public String getPlanNumber() {
		return planNumber;
	}

	public Product getProduct() {
		return product;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public int getTargetQuantity() {
		return targetQuantity;
	}

	public ProductionPlanStatus getStatus() {
		return status;
	}
}
