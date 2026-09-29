package com.pes.user.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pes.auth.PesUserDetailsService;
import com.pes.common.api.PageResponse;
import com.pes.config.SecurityConfig;
import com.pes.user.application.UserService;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private PesUserDetailsService userDetailsService;

	@Test
	void rejectsUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/admin/users"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsWorkerRole() throws Exception {
		mockMvc.perform(get("/api/admin/users").with(user("worker").roles("WORKER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void allowsAdminRole() throws Exception {
		when(userService.search(any(), any(), anyInt(), anyInt()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}
}
