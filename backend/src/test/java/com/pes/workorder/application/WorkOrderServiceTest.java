package com.pes.workorder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.pes.common.error.ForbiddenException;
import com.pes.plan.domain.ProductionPlan;
import com.pes.plan.domain.ProductionPlanRepository;
import com.pes.process.domain.ProductionProcess;
import com.pes.process.domain.ProductionProcessRepository;
import com.pes.product.domain.Product;
import com.pes.product.domain.ProductUnit;
import com.pes.user.domain.UserAccount;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;
import com.pes.workorder.api.WorkOrderDtos;
import com.pes.workorder.domain.WorkOrder;
import com.pes.workorder.domain.WorkOrderRepository;
import com.pes.workorder.domain.WorkOrderStatus;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

	@Mock
	private WorkOrderRepository repository;

	@Mock
	private ProductionPlanRepository planRepository;

	@Mock
	private ProductionProcessRepository processRepository;

	@Mock
	private UserAccountRepository userRepository;

	@Test
	void rejectsQuantityThatExceedsRemainingPlanQuantity() {
		Fixture fixture = fixture();
		fixture.plan.confirm();
		when(planRepository.findByIdForUpdate(fixture.planId)).thenReturn(Optional.of(fixture.plan));
		when(processRepository.findById(fixture.processId)).thenReturn(Optional.of(fixture.process));
		when(userRepository.findById(fixture.workerId)).thenReturn(Optional.of(fixture.worker));
		when(repository.sumTargetQuantityByProductionPlanId(fixture.planId)).thenReturn(80L);
		WorkOrderService service = service();

		assertThatThrownBy(() -> service.create(new WorkOrderDtos.CreateRequest(
				fixture.planId, fixture.processId, fixture.workerId, 21)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("초과");
	}

	@Test
	void onlyAssignedWorkerCanStartWorkOrder() {
		Fixture fixture = fixture();
		WorkOrder workOrder = new WorkOrder("WO-TEST", fixture.plan, fixture.process, fixture.worker, 100);
		UUID workOrderId = UUID.randomUUID();
		ReflectionTestUtils.setField(workOrder, "id", workOrderId);
		when(repository.findByIdForUpdate(workOrderId)).thenReturn(Optional.of(workOrder));
		WorkOrderService service = service();
		PesUserPrincipal otherWorker = new PesUserPrincipal(
				UUID.randomUUID(), "other", "hash", "다른 작업자", UserRole.WORKER, true);

		assertThatThrownBy(() -> service.start(workOrderId, otherWorker))
				.isInstanceOf(ForbiddenException.class)
				.hasMessageContaining("배정된 작업자");
		assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.WAITING);
	}

	@Test
	void preventsDuplicateStartAfterAssignedWorkerStarts() {
		Fixture fixture = fixture();
		WorkOrder workOrder = new WorkOrder("WO-TEST", fixture.plan, fixture.process, fixture.worker, 100);
		UUID workOrderId = UUID.randomUUID();
		ReflectionTestUtils.setField(workOrder, "id", workOrderId);
		when(repository.findByIdForUpdate(workOrderId)).thenReturn(Optional.of(workOrder));
		WorkOrderService service = service();
		PesUserPrincipal principal = new PesUserPrincipal(
				fixture.workerId, "worker", "hash", "작업자", UserRole.WORKER, true);

		service.start(workOrderId, principal);

		assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
		assertThat(workOrder.getStartedAt()).isNotNull();
		verify(repository).flush();
		assertThatThrownBy(() -> service.start(workOrderId, principal))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("대기 상태");
	}

	private WorkOrderService service() {
		return new WorkOrderService(repository, planRepository, processRepository, userRepository);
	}

	private Fixture fixture() {
		UUID planId = UUID.randomUUID();
		UUID processId = UUID.randomUUID();
		UUID workerId = UUID.randomUUID();
		Product product = new Product("PART-001", "정밀 브래킷", ProductUnit.EACH);
		ProductionPlan plan = new ProductionPlan("PP-TEST", product, LocalDate.now(), 100);
		ProductionProcess process = new ProductionProcess("PROC-01", "절삭", null);
		UserAccount worker = new UserAccount("worker", "hash", "작업자", UserRole.WORKER);
		ReflectionTestUtils.setField(plan, "id", planId);
		ReflectionTestUtils.setField(process, "id", processId);
		ReflectionTestUtils.setField(worker, "id", workerId);
		return new Fixture(planId, processId, workerId, plan, process, worker);
	}

	private record Fixture(
			UUID planId,
			UUID processId,
			UUID workerId,
			ProductionPlan plan,
			ProductionProcess process,
			UserAccount worker) {
	}
}
