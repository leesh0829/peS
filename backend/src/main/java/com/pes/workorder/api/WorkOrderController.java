package com.pes.workorder.api;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.workorder.application.WorkOrderService;
import com.pes.workorder.domain.WorkOrderStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

	private final WorkOrderService service;

	public WorkOrderController(WorkOrderService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<WorkOrderDtos.Response> search(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) WorkOrderStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
			@AuthenticationPrincipal PesUserPrincipal principal) {
		return service.search(search, status, page, size, principal);
	}

	@PostMapping
	public ResponseEntity<WorkOrderDtos.Response> create(@Valid @RequestBody WorkOrderDtos.CreateRequest request) {
		WorkOrderDtos.Response response = service.create(request);
		return ResponseEntity.created(URI.create("/api/work-orders/" + response.id())).body(response);
	}

	@PostMapping("/{id}/start")
	public WorkOrderDtos.Response start(
			@PathVariable UUID id,
			@AuthenticationPrincipal PesUserPrincipal principal) {
		return service.start(id, principal);
	}

	@PostMapping("/{id}/complete")
	public WorkOrderDtos.Response complete(
			@PathVariable UUID id,
			@AuthenticationPrincipal PesUserPrincipal principal) {
		return service.complete(id, principal);
	}
}
