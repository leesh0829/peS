package com.pes.user.api;

import java.time.Instant;
import java.util.UUID;

import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	public record CreateRequest(
			@NotBlank
			@Pattern(regexp = "^[a-z0-9._-]{4,50}$", message = "영문 소문자, 숫자, '.', '_', '-'를 사용해 4~50자로 입력해 주세요.")
			String username,
			@NotBlank @Size(min = 10, max = 72) String password,
			@NotBlank @Size(max = 100) String displayName,
			@NotNull UserRole role) {
	}

	public record UpdateRequest(
			@NotBlank @Size(max = 100) String displayName,
			@NotNull UserRole role,
			boolean active,
			long version) {
	}

	public record Response(
			UUID id,
			String username,
			String displayName,
			UserRole role,
			boolean active,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		public static Response from(UserAccount user) {
			return new Response(
					user.getId(),
					user.getUsername(),
					user.getDisplayName(),
					user.getRole(),
					user.isActive(),
					user.getVersion(),
					user.getCreatedAt(),
					user.getUpdatedAt());
		}
	}
}
