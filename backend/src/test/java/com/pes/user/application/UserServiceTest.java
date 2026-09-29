package com.pes.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.pes.common.error.ConflictException;
import com.pes.user.api.UserDtos;
import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserAccountRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void usesEmptyStringWhenSearchTermIsMissing() {
		when(repository.search(anyString(), isNull(), any(Pageable.class))).thenReturn(Page.empty());
		UserService service = new UserService(repository, passwordEncoder);

		service.search(null, null, 0, 20);

		verify(repository).search(eq(""), isNull(), any(Pageable.class));
	}

	@Test
	void hashesPasswordWhenCreatingUser() {
		when(repository.existsByUsername("worker01")).thenReturn(false);
		when(passwordEncoder.encode("secure-pass-1234")).thenReturn("hashed-password");
		when(repository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
		UserService service = new UserService(repository, passwordEncoder);

		UserDtos.Response response = service.create(
				new UserDtos.CreateRequest("worker01", "secure-pass-1234", "작업자 1", UserRole.WORKER));

		assertThat(response.username()).isEqualTo("worker01");
		verify(passwordEncoder).encode("secure-pass-1234");
	}

	@Test
	void preventsRemovingLastActiveAdmin() {
		UUID id = UUID.randomUUID();
		UserAccount admin = new UserAccount("admin01", "hashed", "관리자", UserRole.ADMIN);
		when(repository.findById(id)).thenReturn(Optional.of(admin));
		when(repository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);
		UserService service = new UserService(repository, passwordEncoder);

		assertThatThrownBy(() -> service.update(
				id,
				new UserDtos.UpdateRequest("관리자", UserRole.MANAGER, true, 0)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("마지막 활성 관리자");
	}
}
