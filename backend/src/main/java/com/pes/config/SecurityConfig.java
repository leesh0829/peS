package com.pes.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.spa())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
						.sessionFixation(fixation -> fixation.migrateSession()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/health", "/actuator/health/**", "/api/auth/csrf", "/api/auth/session", "/api/auth/login").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/workers/**", "/api/production-plans/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers("/api/inspection-candidates/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/inspections/**", "/api/defect-codes/**").hasAnyRole("ADMIN", "MANAGER", "WORKER")
						.requestMatchers("/api/inspections/**", "/api/defect-codes/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.POST, "/api/work-orders/*/materials").hasRole("WORKER")
						.requestMatchers(HttpMethod.GET, "/api/material-lots/**", "/api/product-lots/**").hasAnyRole("ADMIN", "MANAGER", "WORKER")
						.requestMatchers("/api/material-lots/**", "/api/product-lots/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.POST, "/api/work-orders/*/start", "/api/work-orders/*/complete", "/api/work-orders/*/results").hasRole("WORKER")
						.requestMatchers(HttpMethod.GET, "/api/work-orders/**").hasAnyRole("ADMIN", "MANAGER", "WORKER")
						.requestMatchers("/api/work-orders/**").hasAnyRole("ADMIN", "MANAGER")
						.requestMatchers(HttpMethod.GET, "/api/production-results/**").hasAnyRole("ADMIN", "MANAGER", "WORKER")
						.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/processes/**").hasAnyRole("ADMIN", "MANAGER", "WORKER")
						.requestMatchers("/api/products/**", "/api/processes/**").hasAnyRole("ADMIN", "MANAGER")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginProcessingUrl("/api/auth/login")
						.successHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value()))
						.failureHandler((request, response, exception) -> writeError(
								response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다.")))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.deleteCookies("JSESSIONID")
						.logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> writeError(
								response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."))
						.accessDeniedHandler((request, response, exception) -> writeError(
								response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "요청을 수행할 권한이 없습니다.")));

		return http.build();
	}

	private static void writeError(HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().printf(
				"{\"status\":%d,\"code\":\"%s\",\"message\":\"%s\",\"fieldErrors\":{}}",
				status.value(), code, message);
	}
}
