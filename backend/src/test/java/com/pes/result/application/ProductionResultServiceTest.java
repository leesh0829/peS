package com.pes.result.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.pes.auth.PesUserPrincipal;
import com.pes.common.error.ConflictException;
import com.pes.common.error.InvalidRequestException;
import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductUnit;
import com.pes.result.api.ProductionResultDtos;
import com.pes.result.domain.ProductionResult;
import com.pes.result.domain.ProductionResultRepository;
import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserRole;
import com.pes.workorder.domain.WorkOrder;
import com.pes.workorder.domain.WorkOrderRepository;

@ExtendWith(MockitoExtension.class)
class ProductionResultServiceTest {

	@Mock
	private ProductionResultRepository repository;

	@Mock
	private WorkOrderRepository workOrderRepository;

	@Test
	void rejectsWhenProducedDoesNotEqualGoodPlusDefect() {
		ProductionResultService service = new ProductionResultService(repository, workOrderRepository);

		assertThatThrownBy(() -> service.create(
				UUID.randomUUID(),
				new ProductionResultDtos.CreateRequest(10, 8, 1),
				principal(UUID.randomUUID())))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("합과 같아야");
		verify(workOrderRepository, never()).findByIdForUpdate(any());
	}

	@Test
	void rejectsNegativeQuantity() {
		ProductionResultService service = new ProductionResultService(repository, workOrderRepository);

		assertThatThrownBy(() -> service.create(
				UUID.randomUUID(),
				new ProductionResultDtos.CreateRequest(1, -1, 2),
				principal(UUID.randomUUID())))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("0 이상");
	}

	@Test
	void rejectsResultThatExceedsWorkOrderTarget() {
		Fixture fixture = fixture();
		when(workOrderRepository.findByIdForUpdate(fixture.workOrderId)).thenReturn(Optional.of(fixture.workOrder));
		when(repository.sumProducedQuantityByWorkOrderId(fixture.workOrderId)).thenReturn(95L);
		ProductionResultService service = new ProductionResultService(repository, workOrderRepository);

		assertThatThrownBy(() -> service.create(
				fixture.workOrderId,
				new ProductionResultDtos.CreateRequest(6, 5, 1),
				principal(fixture.workerId)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("초과");
	}

	@Test
	void rejectsResultAfterWorkOrderCompletion() {
		Fixture fixture = fixture();
		fixture.workOrder.complete(Instant.now());
		when(workOrderRepository.findByIdForUpdate(fixture.workOrderId)).thenReturn(Optional.of(fixture.workOrder));
		ProductionResultService service = new ProductionResultService(repository, workOrderRepository);

		assertThatThrownBy(() -> service.create(
				fixture.workOrderId,
				new ProductionResultDtos.CreateRequest(1, 1, 0),
				principal(fixture.workerId)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("작업 중인");
	}

	@Test
	void recordsValidIncrementalResult() {
		Fixture fixture = fixture();
		when(workOrderRepository.findByIdForUpdate(fixture.workOrderId)).thenReturn(Optional.of(fixture.workOrder));
		when(repository.sumProducedQuantityByWorkOrderId(fixture.workOrderId)).thenReturn(90L);
		when(repository.saveAndFlush(any(ProductionResult.class))).thenAnswer(invocation -> {
			ProductionResult result = invocation.getArgument(0);
			ReflectionTestUtils.setField(result, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(result, "createdAt", Instant.now());
			return result;
		});
		ProductionResultService service = new ProductionResultService(repository, workOrderRepository);

		ProductionResultDtos.Response response = service.create(
				fixture.workOrderId,
				new ProductionResultDtos.CreateRequest(10, 9, 1),
				principal(fixture.workerId));

		assertThat(response.producedQuantity()).isEqualTo(10);
		assertThat(response.goodQuantity() + response.defectQuantity()).isEqualTo(response.producedQuantity());
	}

	private Fixture fixture() {
		UUID workerId = UUID.randomUUID();
		UUID workOrderId = UUID.randomUUID();
		Product product = new Product("PART-001", "정밀 브래킷", ProductUnit.EACH);
		ProductionPlan plan = new ProductionPlan("PP-TEST", product, LocalDate.now(), 100);
		ProductionProcess process = new ProductionProcess("PROC-01", "절삭", null);
		UserAccount worker = new UserAccount("worker", "hash", "작업자", UserRole.WORKER);
		ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(process, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(worker, "id", workerId);
		WorkOrder workOrder = new WorkOrder("WO-TEST", plan, process, worker, 100);
		ReflectionTestUtils.setField(workOrder, "id", workOrderId);
		workOrder.start(Instant.now());
		return new Fixture(workerId, workOrderId, workOrder);
	}

	private PesUserPrincipal principal(UUID workerId) {
		return new PesUserPrincipal(workerId, "worker", "hash", "작업자", UserRole.WORKER, true);
	}

	private record Fixture(UUID workerId, UUID workOrderId, WorkOrder workOrder) {
	}
}
