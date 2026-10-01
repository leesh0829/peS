package com.pes.workorder.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pes.auth.PesUserDetailsService;
import com.pes.common.api.PageResponse;
import com.pes.config.SecurityConfig;
import com.pes.workorder.application.WorkOrderService;

@WebMvcTest(WorkOrderController.class)
@Import(SecurityConfig.class)
class WorkOrderControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WorkOrderService service;

	@MockitoBean
	private PesUserDetailsService userDetailsService;

	@Test
	void workerCanReadWorkOrders() throws Exception {
		when(service.search(any(), any(), anyInt(), anyInt(), any()))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/work-orders").with(user("worker").roles("WORKER")))
				.andExpect(status().isOk());
	}

	@Test
	void workerCannotCreateWorkOrder() throws Exception {
		mockMvc.perform(post("/api/work-orders")
				.with(user("worker").roles("WORKER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}
}
