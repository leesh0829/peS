package com.pes.user.domain;

import com.pes.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class UserAccount extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 100)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(nullable = false)
	private boolean active;

	protected UserAccount() {
	}

	public UserAccount(String username, String passwordHash, String displayName, UserRole role) {
		this.username = username;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
		this.active = true;
	}

	public void update(String displayName, UserRole role, boolean active) {
		this.displayName = displayName;
		this.role = role;
		this.active = active;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserRole getRole() {
		return role;
	}

	public boolean isActive() {
		return active;
	}
}
