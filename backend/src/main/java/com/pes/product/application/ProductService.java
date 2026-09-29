package com.pes.product.application;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.NotFoundException;
import com.pes.product.api.ProductDtos;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository repository;

	public ProductService(ProductRepository repository) {
		this.repository = repository;
	}

	public PageResponse<ProductDtos.Response> search(String search, Boolean active, int page, int size) {
		String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
		PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("code").ascending());
		return PageResponse.from(repository.search(normalizedSearch, active, pageable), ProductDtos.Response::from);
	}

	@Transactional
	public ProductDtos.Response create(ProductDtos.CreateRequest request) {
		if (repository.existsByCode(request.code())) {
			throw new ConflictException("이미 사용 중인 품목 코드입니다.");
		}
		return ProductDtos.Response.from(repository.save(new Product(request.code(), request.name(), request.unit())));
	}

	@Transactional
	public ProductDtos.Response update(UUID id, ProductDtos.UpdateRequest request) {
		Product product = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("품목을 찾을 수 없습니다."));
		if (product.getVersion() != request.version()) {
			throw new ConflictException("다른 사용자가 먼저 변경했습니다. 새로고침 후 다시 시도해 주세요.");
		}
		product.update(request.name(), request.unit(), request.active());
		return ProductDtos.Response.from(product);
	}
}
