package com.pes.quality.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.error.*;
import com.pes.lot.domain.*;
import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.product.domain.*;
import com.pes.quality.api.QualityDtos;
import com.pes.quality.domain.*;
import com.pes.result.domain.ProductionResult;
import com.pes.user.domain.*;
import com.pes.workorder.domain.*;

@ExtendWith(MockitoExtension.class)
class QualityServiceTest {
    @Mock DefectCodeRepository codes;
    @Mock InspectionResultRepository inspections;
    @Mock ProductLotRepository lots;
    @Mock UserAccountRepository users;
    private final UUID lotId = UUID.randomUUID();
    private final UUID inspectorId = UUID.randomUUID();
    private final UUID codeId = UUID.randomUUID();
    private final UUID workerId = UUID.randomUUID();
    private QualityService service() { return new QualityService(codes, inspections, lots, users); }
    private PesUserPrincipal principal(UserRole role) {
        return new PesUserPrincipal(inspectorId, "manager", "hash", "검사자", role, true);
    }
    private QualityDtos.CreateInspectionRequest request(int inspected, int accepted, int rejected) {
        return new QualityDtos.CreateInspectionRequest(inspected, accepted,
            rejected == 0 ? List.of() : List.of(new QualityDtos.DefectRequest(codeId, rejected)), null);
    }
    private ProductLot fixture(boolean completed, int good) {
        var worker = new UserAccount("worker", "hash", "작업자", UserRole.WORKER);
        ReflectionTestUtils.setField(worker, "id", workerId);
        var product = new Product("PART-01", "가상 부품", ProductUnit.EACH);
        var plan = new ProductionPlan("PP-TEST", product, LocalDate.now(), 10);
        var order = new WorkOrder("WO-TEST", plan, new ProductionProcess("CUT-01", "절삭", null), worker, 10);
        order.start(Instant.now());
        if (completed) order.complete(Instant.now());
        var production = new ProductionResult(order, 10, good, 10 - good, worker);
        var lot = new ProductLot("PL-TEST", production);
        ReflectionTestUtils.setField(lot, "id", lotId);
        when(lots.findByIdForUpdate(lotId)).thenReturn(Optional.of(lot));
        return lot;
    }
    @Test void rejectsQuantityMismatchBeforeAccessingDatabase() {
        assertThatThrownBy(() -> service().inspect(lotId, request(10, 8, 1), principal(UserRole.MANAGER)))
            .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(lots, inspections, codes, users);
    }
    @Test void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> service().inspect(lotId, request(10, -1, 11), principal(UserRole.MANAGER)))
            .isInstanceOf(InvalidRequestException.class);
    }
    @Test void rejectsDuplicateDefectCode() {
        var request = new QualityDtos.CreateInspectionRequest(10, 8, List.of(
            new QualityDtos.DefectRequest(codeId, 1), new QualityDtos.DefectRequest(codeId, 1)), null);
        assertThatThrownBy(() -> service().inspect(lotId, request, principal(UserRole.MANAGER))).isInstanceOf(InvalidRequestException.class);
    }
    @Test void rejectsWorkerWrite() {
        assertThatThrownBy(() -> service().inspect(lotId, request(10, 10, 0), principal(UserRole.WORKER))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(lots, inspections, codes, users);
    }
    @Test void rejectsInspectionBeforeCompletion() {
        fixture(false, 10);
        assertThatThrownBy(() -> service().inspect(lotId, request(10, 10, 0), principal(UserRole.MANAGER))).isInstanceOf(ConflictException.class);
    }
    @Test void rejectsRepeatedInspection() {
        fixture(true, 10);
        when(inspections.existsByProductLotId(lotId)).thenReturn(true);
        assertThatThrownBy(() -> service().inspect(lotId, request(10, 10, 0), principal(UserRole.MANAGER))).isInstanceOf(ConflictException.class);
    }
    @Test void rejectsPartialInspection() {
        fixture(true, 10);
        assertThatThrownBy(() -> service().inspect(lotId, request(9, 9, 0), principal(UserRole.MANAGER))).isInstanceOf(InvalidRequestException.class);
    }
    @Test void cannotAcceptKnownProductionDefects() {
        fixture(true, 9);
        assertThatThrownBy(() -> service().inspect(lotId, request(10, 10, 0), principal(UserRole.MANAGER)))
            .isInstanceOf(InvalidRequestException.class).hasMessageContaining("생산실적");
    }
    private void saveStub() {
        when(users.findById(inspectorId)).thenReturn(Optional.of(new UserAccount("manager", "hash", "검사자", UserRole.MANAGER)));
        when(inspections.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
    @Test void recordsPassWithoutChangingProductionQuantities() {
        var lot = fixture(true, 10); saveStub();
        var response = service().inspect(lotId, request(10, 10, 0), principal(UserRole.MANAGER));
        assertThat(response.judgement()).isEqualTo(InspectionJudgement.PASS);
        assertThat(response.defects()).isEmpty();
        assertThat(lot.getProductionResult().getGoodQuantity()).isEqualTo(10);
    }
    @Test void recordsMultipleDefectsAndDerivesFailure() {
        var lot = fixture(true, 9); saveStub();
        UUID second = UUID.randomUUID();
        when(codes.findById(codeId)).thenReturn(Optional.of(new DefectCode("SCRATCH", "가상 흠집")));
        when(codes.findById(second)).thenReturn(Optional.of(new DefectCode("DIMENSION", "가상 치수")));
        var request = new QualityDtos.CreateInspectionRequest(10, 8, List.of(
            new QualityDtos.DefectRequest(codeId, 1), new QualityDtos.DefectRequest(second, 1)), "가상 검사");
        var response = service().inspect(lotId, request, principal(UserRole.MANAGER));
        assertThat(response.judgement()).isEqualTo(InspectionJudgement.FAIL);
        assertThat(response.rejectedQuantity()).isEqualTo(2);
        assertThat(response.defects()).hasSize(2);
        assertThat(lot.getProductionResult().getGoodQuantity()).isEqualTo(9);
        assertThat(lot.getProductionResult().getWorkOrder().getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
    }
    @Test void rejectsZeroDefectQuantity() {
        var request = new QualityDtos.CreateInspectionRequest(10, 10, List.of(new QualityDtos.DefectRequest(codeId, 0)), null);
        assertThatThrownBy(() -> service().inspect(lotId, request, principal(UserRole.MANAGER))).isInstanceOf(InvalidRequestException.class);
    }
    @Test void doesNotOverflowDefectSum() {
        var request = new QualityDtos.CreateInspectionRequest(10, 0, List.of(
            new QualityDtos.DefectRequest(codeId, Integer.MAX_VALUE),
            new QualityDtos.DefectRequest(UUID.randomUUID(), Integer.MAX_VALUE)), null);
        assertThatThrownBy(() -> service().inspect(lotId, request, principal(UserRole.MANAGER))).isInstanceOf(InvalidRequestException.class);
    }
    @Test void preventsOtherWorkerReadingInspectionByLotId() {
        var lot = fixture(true, 10);
        reset(lots);
        when(lots.findDetailById(lotId)).thenReturn(Optional.of(lot));
        assertThatThrownBy(() -> service().forLot(lotId, principal(UserRole.WORKER))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(inspections);
    }
}
