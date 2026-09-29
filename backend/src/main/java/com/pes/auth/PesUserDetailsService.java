package com.pes.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pes.user.domain.UserAccountRepository;

@Service
public class PesUserDetailsService implements UserDetailsService {

	private final UserAccountRepository repository;

	public PesUserDetailsService(UserAccountRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return repository.findByUsername(username)
				.map(PesUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
	}
}
