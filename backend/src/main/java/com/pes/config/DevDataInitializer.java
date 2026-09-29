package com.pes.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;

@Component
@Profile("dev")
public class DevDataInitializer implements ApplicationRunner {

	private static final String DEMO_PASSWORD = "pes-demo-1234";

	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;

	public DevDataInitializer(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		createIfMissing("admin", "관리자", UserRole.ADMIN);
		createIfMissing("manager", "생산관리자", UserRole.MANAGER);
		createIfMissing("worker", "작업자", UserRole.WORKER);
	}

	private void createIfMissing(String username, String displayName, UserRole role) {
		if (!repository.existsByUsername(username)) {
			repository.save(new UserAccount(username, passwordEncoder.encode(DEMO_PASSWORD), displayName, role));
		}
	}
}
