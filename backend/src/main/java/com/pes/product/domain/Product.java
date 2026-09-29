package com.pes.product.domain;

import com.pes.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product extends BaseEntity {

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ProductUnit unit;

	@Column(nullable = false)
	private boolean active;

	protected Product() {
	}

	public Product(String code, String name, ProductUnit unit) {
		this.code = code;
		this.name = name;
		this.unit = unit;
		this.active = true;
	}

	public void update(String name, ProductUnit unit, boolean active) {
		this.name = name;
		this.unit = unit;
		this.active = active;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public ProductUnit getUnit() {
		return unit;
	}

	public boolean isActive() {
		return active;
	}
}
