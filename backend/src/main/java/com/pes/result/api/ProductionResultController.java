package com.pes.result.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.result.application.ProductionResultService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
public class ProductionResultController {

	private final ProductionResultService service;

	public ProductionResultController(ProductionResultService service) {
		this.service = service;
	}

	@GetMapping("/api/production-results")
	public PageResponse<ProductionResultDtos.Response> search(
			@RequestParam(required = false) String search,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@AuthenticationPrincipal PesUserPrincipal principal) {
		return service.search(search, page, size, principal);
	}

	@PostMapping("/api/work-orders/{workOrderId}/results")
	public ResponseEntity<ProductionResultDtos.Response> create(
			@org.springframework.web.bind.annotation.PathVariable UUID workOrderId,
			@Valid @RequestBody ProductionResultDtos.CreateRequest request,
			@AuthenticationPrincipal PesUserPrincipal principal) {
		ProductionResultDtos.Response response = service.create(workOrderId, request, principal);
		return ResponseEntity.created(URI.create("/api/production-results/" + response.id())).body(response);
	}
}
