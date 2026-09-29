package com.pes.auth;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserRole;

public record PesUserPrincipal(
		UUID id,
		String username,
		String password,
		String displayName,
		UserRole role,
		boolean enabled) implements UserDetails {

	public static PesUserPrincipal from(UserAccount user) {
		return new PesUserPrincipal(
				user.getId(),
				user.getUsername(),
				user.getPasswordHash(),
				user.getDisplayName(),
				user.getRole(),
				user.isActive());
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
