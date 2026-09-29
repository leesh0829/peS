package com.pes.product.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pes.common.api.PageResponse;
import com.pes.product.application.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductDtos.Response> search(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.search(search, active, page, size);
	}

	@PostMapping
	public ResponseEntity<ProductDtos.Response> create(@Valid @RequestBody ProductDtos.CreateRequest request) {
		ProductDtos.Response response = service.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + response.id())).body(response);
	}

	@PutMapping("/{id}")
	public ProductDtos.Response update(@PathVariable UUID id, @Valid @RequestBody ProductDtos.UpdateRequest request) {
		return service.update(id, request);
	}
}
