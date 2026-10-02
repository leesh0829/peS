package com.pes.label;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.error.*;
import com.pes.lot.domain.*;
import com.pes.plan.domain.ProductionPlan;
import com.pes.process.domain.ProductionProcess;
import com.pes.product.domain.*;
import com.pes.result.domain.ProductionResult;
import com.pes.user.domain.*;
import com.pes.workorder.domain.WorkOrder;

class LabelServiceTest {
    private final ProductLotRepository lots = mock(ProductLotRepository.class);
    private final LabelEventRepository events = mock(LabelEventRepository.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final UUID id = UUID.randomUUID(), userId = UUID.randomUUID();
    private final UserAccount user = new UserAccount("manager", "hash", "가상 관리자", UserRole.MANAGER);
    private final LabelService service = new LabelService(lots, events, users);
    private PesUserPrincipal principal(UserRole role) { return new PesUserPrincipal(userId, "demo", "hash", "시연", role, true); }
    private ProductLot lot(boolean completed) {
        var plan = new ProductionPlan("PP-TEST", new Product("PART-01", "가상 부품", ProductUnit.EACH), LocalDate.now(), 10);
        var order = new WorkOrder("WO-TEST", plan, new ProductionProcess("CUT-01", "가상 공정", null), user, 10);
        order.start(Instant.now());
        if (completed) order.complete(Instant.now());
        var lot = new ProductLot("PL-TEST", new ProductionResult(order, 10, 9, 1, user));
        when(lots.findByIdForUpdate(id)).thenReturn(Optional.of(lot));
        return lot;
    }
    @Test void workerCannotIssue() {
        assertThatThrownBy(() -> service.issue(id, false, null, principal(UserRole.WORKER))).isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(lots, events, users);
    }
    @Test void reprintNeedsReason() {
        assertThatThrownBy(() -> service.issue(id, true, "   ", principal(UserRole.MANAGER))).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(lots, events);
    }
    @Test void incompleteOrderCannotIssue() {
        lot(false);
        assertThatThrownBy(() -> service.issue(id, false, null, principal(UserRole.MANAGER))).isInstanceOf(ConflictException.class);
    }
    @Test void reprintRequiresOriginal() {
        lot(true); when(events.findFirstByProductLotIdOrderBySequenceNumberDesc(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issue(id, true, "훼손", principal(UserRole.MANAGER))).isInstanceOf(ConflictException.class);
    }
    @Test void duplicateIssueIsRejected() {
        var lot = lot(true);
        when(events.findFirstByProductLotIdOrderBySequenceNumberDesc(id)).thenReturn(Optional.of(new LabelEvent(lot, user)));
        assertThatThrownBy(() -> service.issue(id, false, null, principal(UserRole.MANAGER))).isInstanceOf(ConflictException.class);
    }
    @Test void issueSnapshotsProductionWithoutInventoryChange() {
        var lot = lot(true); when(events.findFirstByProductLotIdOrderBySequenceNumberDesc(id)).thenReturn(Optional.empty());
        when(users.findById(userId)).thenReturn(Optional.of(user)); when(events.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.issue(id, false, null, principal(UserRole.MANAGER));
        assertThat(response.sequenceNumber()).isEqualTo(1); assertThat(response.reason()).isNull();
        assertThat(response.productName()).isEqualTo("가상 부품"); assertThat(response.producedQuantity()).isEqualTo(10);
        assertThat(lot.getProductionResult().getGoodQuantity()).isEqualTo(9);
    }
    @Test void reprintCopiesOriginalSnapshotAndRecordsReason() {
        var lot = lot(true); var original = new LabelEvent(lot, user);
        when(events.findFirstByProductLotIdOrderBySequenceNumberDesc(id)).thenReturn(Optional.of(original));
        when(users.findById(userId)).thenReturn(Optional.of(user)); when(events.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var response = service.issue(id, true, "  훼손  ", principal(UserRole.MANAGER));
        assertThat(response.sequenceNumber()).isEqualTo(2); assertThat(response.reason()).isEqualTo("훼손");
        assertThat(response.productCode()).isEqualTo(original.getProductCode());
    }
}
