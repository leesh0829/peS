package com.pes.process.domain;

import com.pes.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_process")
public class ProductionProcess extends BaseEntity {

	@Column(nullable = false, unique = true, length = 30)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private boolean active;

	protected ProductionProcess() {
	}

	public ProductionProcess(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = normalizeDescription(description);
		this.active = true;
	}

	public void update(String name, String description, boolean active) {
		this.name = name;
		this.description = normalizeDescription(description);
		this.active = active;
	}

	private String normalizeDescription(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean isActive() {
		return active;
	}
}
