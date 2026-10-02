package com.pes.label;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.common.error.*;
import com.pes.lot.domain.ProductLotRepository;
import com.pes.user.domain.*;
import com.pes.workorder.domain.WorkOrderStatus;

@Service
@Transactional(readOnly = true)
public class LabelService {
    public record Response(UUID id, int sequenceNumber, String lotNumber, String productCode,
        String productName, int producedQuantity, String issuerName, String reason, Instant createdAt) {
        static Response from(LabelEvent e) { return new Response(e.getId(), e.getSequenceNumber(), e.getLotNumber(),
            e.getProductCode(), e.getProductName(), e.getProducedQuantity(), e.getIssuerName(), e.getReason(), e.getCreatedAt()); }
    }
    private final ProductLotRepository lots;
    private final LabelEventRepository events;
    private final UserAccountRepository users;
    public LabelService(ProductLotRepository lots, LabelEventRepository events, UserAccountRepository users) {
        this.lots = lots; this.events = events; this.users = users;
    }
    public PageResponse<Response> history(UUID id, int page, PesUserPrincipal principal) {
        var lot = lots.findDetailById(id).orElseThrow(() -> new NotFoundException("생산 LOT를 찾을 수 없습니다."));
        if (principal.role() == UserRole.WORKER && !lot.getProductionResult().getWorkOrder().getAssignedWorker().getId().equals(principal.id()))
            throw new ForbiddenException("본인 작업지시의 라벨 이력만 조회할 수 있습니다.");
        return PageResponse.from(events.findByProductLotId(id, PageRequest.of(page, 20, Sort.by("sequenceNumber").descending())), Response::from);
    }
    @Transactional
    public Response issue(UUID id, boolean reprint, String reason, PesUserPrincipal principal) {
        if (principal.role() == UserRole.WORKER) throw new ForbiddenException("관리자와 생산관리자만 라벨을 발행할 수 있습니다.");
        if (reprint && (reason == null || reason.isBlank() || reason.trim().length() > 500))
            throw new InvalidRequestException("재출력 사유를 1~500자로 입력해 주세요.");
        var lot = lots.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("생산 LOT를 찾을 수 없습니다."));
        if (lot.getProductionResult().getWorkOrder().getStatus() != WorkOrderStatus.COMPLETED)
            throw new ConflictException("작업 완료 후에만 LOT 식별 라벨을 발행할 수 있습니다.");
        var latest = events.findFirstByProductLotIdOrderBySequenceNumberDesc(id);
        if (reprint && latest.isEmpty()) throw new ConflictException("최초 발행 후 재출력할 수 있습니다.");
        if (!reprint && latest.isPresent()) throw new ConflictException("이미 발행했습니다. 재출력 명령을 사용해 주세요.");
        var user = users.findById(principal.id()).orElseThrow(() -> new NotFoundException("발행자를 찾을 수 없습니다."));
        if (latest.isPresent() && latest.get().getSequenceNumber() == Integer.MAX_VALUE) throw new ConflictException("발행 횟수 한도를 초과했습니다.");
        var event = reprint ? new LabelEvent(latest.orElseThrow(), latest.get().getSequenceNumber() + 1, user, reason.trim()) : new LabelEvent(lot, user);
        return Response.from(events.saveAndFlush(event));
    }
}
