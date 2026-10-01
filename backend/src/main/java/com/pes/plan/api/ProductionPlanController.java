package com.pes.plan.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pes.common.api.PageResponse;
import com.pes.plan.application.ProductionPlanService;
import com.pes.plan.domain.ProductionPlanStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController {

	private final ProductionPlanService service;

	public ProductionPlanController(ProductionPlanService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductionPlanDtos.Response> search(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) ProductionPlanStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return service.search(search, status, page, size);
	}

	@PostMapping
	public ResponseEntity<ProductionPlanDtos.Response> create(
			@Valid @RequestBody ProductionPlanDtos.CreateRequest request) {
		ProductionPlanDtos.Response response = service.create(request);
		return ResponseEntity.created(URI.create("/api/production-plans/" + response.id())).body(response);
	}

	@PostMapping("/{id}/confirm")
	public ProductionPlanDtos.Response confirm(@PathVariable UUID id) {
		return service.confirm(id);
	}
}
