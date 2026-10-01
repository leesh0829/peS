package com.pes.user.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.user.api.WorkerDtos;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;

@Service
@Transactional(readOnly = true)
public class WorkerQueryService {

	private final UserAccountRepository repository;

	public WorkerQueryService(UserAccountRepository repository) {
		this.repository = repository;
	}

	public List<WorkerDtos.Response> findActiveWorkers() {
		return repository.findAllByRoleAndActiveTrueOrderByDisplayNameAsc(UserRole.WORKER)
				.stream()
				.map(WorkerDtos.Response::from)
				.toList();
	}
}
