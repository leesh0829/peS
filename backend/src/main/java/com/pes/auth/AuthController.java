package com.pes.auth;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pes.user.domain.UserRole;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@GetMapping("/csrf")
	public CsrfResponse csrf(CsrfToken token) {
		return new CsrfResponse(token.getHeaderName(), token.getParameterName(), token.getToken());
	}

	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal PesUserPrincipal principal) {
		return new CurrentUserResponse(
				principal.id(),
				principal.username(),
				principal.displayName(),
				principal.role());
	}

	public record CsrfResponse(String headerName, String parameterName, String token) {
	}

	public record CurrentUserResponse(UUID id, String username, String displayName, UserRole role) {
	}
}
