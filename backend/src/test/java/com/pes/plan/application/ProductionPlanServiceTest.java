package com.pes.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.pes.common.error.ConflictException;
import com.pes.plan.domain.ProductionPlan;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.plan.domain.ProductionPlanStatus;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductRepository;
import com.pes.product.domain.ProductUnit;
import com.pes.workorder.domain.WorkOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceTest {

	@Mock
	private ProductionPlanRepository repository;

	@Mock
	private ProductRepository productRepository;

	@Mock
	private WorkOrderRepository workOrderRepository;

	@Test
	void rejectsDuplicateConfirmation() {
		UUID planId = UUID.randomUUID();
		Product product = new Product("PART-001", "정밀 브래킷", ProductUnit.EACH);
		ProductionPlan plan = new ProductionPlan("PP-TEST", product, java.time.LocalDate.now(), 100);
		plan.confirm();
		when(repository.findByIdForUpdate(planId)).thenReturn(Optional.of(plan));
		ProductionPlanService service = new ProductionPlanService(repository, productRepository, workOrderRepository);

		assertThatThrownBy(() -> service.confirm(planId))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("생산계획만 확정");
		assertThat(plan.getStatus()).isEqualTo(ProductionPlanStatus.CONFIRMED);
	}
}
