package com.pes.user.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pes.user.application.WorkerQueryService;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

	private final WorkerQueryService service;

	public WorkerController(WorkerQueryService service) {
		this.service = service;
	}

	@GetMapping
	public List<WorkerDtos.Response> findActiveWorkers() {
		return service.findActiveWorkers();
	}
}
