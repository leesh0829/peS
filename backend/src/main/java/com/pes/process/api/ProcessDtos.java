package com.pes.process.api;

import java.time.Instant;
import java.util.UUID;

import com.pes.process.domain.ProductionProcess;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProcessDtos {

	private ProcessDtos() {
	}

	public record CreateRequest(
			@NotBlank
			@Pattern(regexp = "^[A-Z0-9_-]{2,30}$", message = "영문 대문자, 숫자, '_', '-'를 사용해 2~30자로 입력해 주세요.")
			String code,
			@NotBlank @Size(max = 100) String name,
			@Size(max = 500) String description) {
	}

	public record UpdateRequest(
			@NotBlank @Size(max = 100) String name,
			@Size(max = 500) String description,
			boolean active,
			long version) {
	}

	public record Response(
			UUID id,
			String code,
			String name,
			String description,
			boolean active,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		public static Response from(ProductionProcess process) {
			return new Response(
					process.getId(),
					process.getCode(),
					process.getName(),
					process.getDescription(),
					process.isActive(),
					process.getVersion(),
					process.getCreatedAt(),
					process.getUpdatedAt());
		}
	}
}
