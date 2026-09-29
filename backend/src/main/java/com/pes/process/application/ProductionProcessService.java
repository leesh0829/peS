package com.pes.process.application;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.NotFoundException;
import com.pes.process.api.ProcessDtos;
import com.pes.process.domain.ProductionProcess;
import com.pes.process.domain.ProductionProcessRepository;

@Service
@Transactional(readOnly = true)
public class ProductionProcessService {

	private final ProductionProcessRepository repository;

	public ProductionProcessService(ProductionProcessRepository repository) {
		this.repository = repository;
	}

	public PageResponse<ProcessDtos.Response> search(String search, Boolean active, int page, int size) {
		String normalizedSearch = search == null ? "" : search.trim();
		PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("code").ascending());
		return PageResponse.from(repository.search(normalizedSearch, active, pageable), ProcessDtos.Response::from);
	}

	@Transactional
	public ProcessDtos.Response create(ProcessDtos.CreateRequest request) {
		if (repository.existsByCode(request.code())) {
			throw new ConflictException("이미 사용 중인 공정 코드입니다.");
		}
		ProductionProcess process = new ProductionProcess(request.code(), request.name(), request.description());
		return ProcessDtos.Response.from(repository.save(process));
	}

	@Transactional
	public ProcessDtos.Response update(UUID id, ProcessDtos.UpdateRequest request) {
		ProductionProcess process = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("공정을 찾을 수 없습니다."));
		if (process.getVersion() != request.version()) {
			throw new ConflictException("다른 사용자가 먼저 변경했습니다. 새로고침 후 다시 시도해 주세요.");
		}
		process.update(request.name(), request.description(), request.active());
		return ProcessDtos.Response.from(process);
	}
}
