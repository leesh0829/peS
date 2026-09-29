package com.pes.system.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

	private final HealthController controller = new HealthController();

	@Test
	void reportsApiAsUp() {
		HealthResponse response = controller.health();

		assertThat(response.status()).isEqualTo("UP");
		assertThat(response.service()).isEqualTo("peS API");
	}
}
