package com.pes.product.api;

import java.time.Instant;
import java.util.UUID;

import com.pes.product.domain.Product;
import com.pes.product.domain.ProductUnit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ProductDtos {

	private ProductDtos() {
	}

	public record CreateRequest(
			@NotBlank
			@Pattern(regexp = "^[A-Z0-9_-]{2,30}$", message = "영문 대문자, 숫자, '_', '-'를 사용해 2~30자로 입력해 주세요.")
			String code,
			@NotBlank @Size(max = 100) String name,
			@NotNull ProductUnit unit) {
	}

	public record UpdateRequest(
			@NotBlank @Size(max = 100) String name,
			@NotNull ProductUnit unit,
			boolean active,
			long version) {
	}

	public record Response(
			UUID id,
			String code,
			String name,
			ProductUnit unit,
			boolean active,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		public static Response from(Product product) {
			return new Response(
					product.getId(),
					product.getCode(),
					product.getName(),
					product.getUnit(),
					product.isActive(),
					product.getVersion(),
					product.getCreatedAt(),
					product.getUpdatedAt());
		}
	}
}
