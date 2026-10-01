package com.pes.result.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pes.auth.PesUserDetailsService;
import com.pes.config.SecurityConfig;
import com.pes.result.application.ProductionResultService;

@WebMvcTest(ProductionResultController.class)
@Import(SecurityConfig.class)
class ProductionResultControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductionResultService service;

	@MockitoBean
	private PesUserDetailsService userDetailsService;

	@Test
	void managerCannotRecordProductionResult() throws Exception {
		mockMvc.perform(post("/api/work-orders/{id}/results", UUID.randomUUID())
				.with(user("manager").roles("MANAGER"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"producedQuantity\":10,\"goodQuantity\":10,\"defectQuantity\":0}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void workerCannotReadManagementDashboard() throws Exception {
		mockMvc.perform(get("/api/dashboard/summary").with(user("worker").roles("WORKER")))
				.andExpect(status().isForbidden());
	}
}
