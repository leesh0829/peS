package com.pes.lot.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
import com.pes.common.error.*;
import com.pes.lot.api.LotDtos;
import com.pes.lot.domain.*;
import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.product.domain.*;
import com.pes.user.domain.*;
import com.pes.workorder.domain.*;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {
    @Mock MaterialLotRepository materials;
    @Mock WorkOrderMaterialRepository inputs;
    @Mock ProductLotRepository products;
    @Mock WorkOrderRepository orders;
    private final UUID orderId = UUID.randomUUID();
    private final UUID materialId = UUID.randomUUID();
    private final UUID workerId = UUID.randomUUID();
    private LotService service() { return new LotService(materials, inputs, products, orders); }
    private PesUserPrincipal principal(UUID id) { return new PesUserPrincipal(id, "worker", "hash", "작업자", UserRole.WORKER, true); }
    private WorkOrder order(boolean tracked) {
        var worker = new UserAccount("worker", "hash", "작업자", UserRole.WORKER);
        ReflectionTestUtils.setField(worker, "id", workerId);
        var plan = new ProductionPlan("PP-TEST", new Product("PART-01", "가상 부품", ProductUnit.EACH), LocalDate.now(), 10);
        var order = new WorkOrder("WO-TEST", plan, new ProductionProcess("CUT-01", "절삭", null), worker, 10);
        ReflectionTestUtils.setField(order, "id", orderId);
        if (tracked) order.enableLotTracking();
        when(orders.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
        return order;
    }
    private void material() {
        var lot = new MaterialLot("ML-TEST", "RAW-01", "가상 소재", 10);
        ReflectionTestUtils.setField(lot, "id", materialId);
        when(materials.findByIdForUpdate(materialId)).thenReturn(Optional.of(lot));
    }
    private void input(int quantity, UUID id) { service().input(orderId, new LotDtos.InputRequest(materialId, quantity), principal(id)); }
    @Test void rejectsNonPositiveInput() {
        assertThatThrownBy(() -> input(0, workerId)).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(orders, materials, inputs);
    }
    @Test void rejectsOtherWorker() {
        order(true);
        assertThatThrownBy(() -> input(1, UUID.randomUUID())).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(materials, inputs);
    }
    @Test void rejectsLegacyOrderInput() {
        order(false);
        assertThatThrownBy(() -> input(1, workerId)).isInstanceOf(ConflictException.class);
    }
    @Test void freezesInputsAfterStart() {
        order(true).start(Instant.now());
        assertThatThrownBy(() -> input(1, workerId)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(materials, inputs);
    }
    @Test void rejectsDuplicateLot() {
        order(true); material();
        when(inputs.existsByWorkOrderIdAndMaterialLotId(orderId, materialId)).thenReturn(true);
        assertThatThrownBy(() -> input(1, workerId)).isInstanceOf(ConflictException.class).hasMessageContaining("이미 투입");
    }
    @Test void rejectsOrderTargetOverflow() {
        order(true); material();
        when(inputs.sumByWorkOrderId(orderId)).thenReturn(9L);
        assertThatThrownBy(() -> input(2, workerId)).isInstanceOf(ConflictException.class).hasMessageContaining("목표수량");
    }
    @Test void rejectsSharedMaterialLotOverflow() {
        order(true); material();
        when(inputs.sumByMaterialLotId(materialId)).thenReturn(9L);
        assertThatThrownBy(() -> input(2, workerId)).isInstanceOf(ConflictException.class).hasMessageContaining("등록수량");
        verify(inputs, never()).saveAndFlush(any());
    }
    @Test void savesValidInput() {
        order(true); material();
        when(inputs.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service().input(orderId, new LotDtos.InputRequest(materialId, 4), principal(workerId));
        assertThat(response.inputQuantity()).isEqualTo(4);
        assertThat(response.materialLot().lotNumber()).isEqualTo("ML-TEST");
    }
    @Test void hidesOtherWorkersProductTrace() {
        var order = order(true);
        // This test uses the read path, not the write stub.
        reset(orders);
        var result = new com.pes.result.domain.ProductionResult(order, 1, 1, 0, order.getAssignedWorker());
        var lot = new ProductLot("PL-TEST", result);
        when(products.findDetailById(materialId)).thenReturn(Optional.of(lot));
        assertThatThrownBy(() -> service().traceProduct(materialId, principal(UUID.randomUUID()))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(inputs);
    }
}
