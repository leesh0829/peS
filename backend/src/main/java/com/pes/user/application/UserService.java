package com.pes.user.application;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.common.api.PageResponse;
import com.pes.common.error.ConflictException;
import com.pes.common.error.NotFoundException;
import com.pes.user.api.UserDtos;
import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;

@Service
@Transactional(readOnly = true)
public class UserService {

	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	public PageResponse<UserDtos.Response> search(String search, Boolean active, int page, int size) {
		String normalizedSearch = search == null ? "" : search.trim();
		PageRequest pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
		return PageResponse.from(repository.search(normalizedSearch, active, pageable), UserDtos.Response::from);
	}

	@Transactional
	public UserDtos.Response create(UserDtos.CreateRequest request) {
		if (repository.existsByUsername(request.username())) {
			throw new ConflictException("이미 사용 중인 사용자 아이디입니다.");
		}

		UserAccount user = new UserAccount(
				request.username(),
				passwordEncoder.encode(request.password()),
				request.displayName(),
				request.role());
		return UserDtos.Response.from(repository.save(user));
	}

	@Transactional
	public UserDtos.Response update(UUID id, UserDtos.UpdateRequest request) {
		UserAccount user = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

		if (user.getVersion() != request.version()) {
			throw new ConflictException("다른 사용자가 먼저 변경했습니다. 새로고침 후 다시 시도해 주세요.");
		}

		boolean removesLastAdmin = user.getRole() == UserRole.ADMIN
				&& (request.role() != UserRole.ADMIN || !request.active())
				&& repository.countByRoleAndActiveTrue(UserRole.ADMIN) == 1;
		if (removesLastAdmin) {
			throw new ConflictException("마지막 활성 관리자 계정은 비활성화하거나 역할을 변경할 수 없습니다.");
		}

		user.update(request.displayName(), request.role(), request.active());
		return UserDtos.Response.from(user);
	}
}
