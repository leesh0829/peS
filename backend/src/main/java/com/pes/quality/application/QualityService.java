package com.pes.quality.application;

import java.util.HashSet;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.common.error.*;
import com.pes.lot.api.LotDtos;
import com.pes.lot.domain.ProductLotRepository;
import com.pes.quality.api.QualityDtos;
import com.pes.quality.domain.*;
import com.pes.user.domain.UserAccountRepository;
import com.pes.user.domain.UserRole;
import com.pes.workorder.domain.WorkOrderStatus;

@Service
@Transactional(readOnly = true)
public class QualityService {
    private final DefectCodeRepository codes;
    private final InspectionResultRepository inspections;
    private final ProductLotRepository lots;
    private final UserAccountRepository users;
    public QualityService(DefectCodeRepository codes, InspectionResultRepository inspections,
        ProductLotRepository lots, UserAccountRepository users) {
        this.codes = codes; this.inspections = inspections; this.lots = lots; this.users = users;
    }
    public PageResponse<QualityDtos.CodeResponse> searchCodes(String search, int page, int size) {
        return PageResponse.from(codes.search(normalize(search), pageable(page, size)), QualityDtos.CodeResponse::from);
    }
    @Transactional
    public QualityDtos.CodeResponse createCode(QualityDtos.CreateCodeRequest request) {
        return QualityDtos.CodeResponse.from(codes.saveAndFlush(new DefectCode(request.code(), request.name().trim())));
    }
    public PageResponse<LotDtos.ProductResponse> candidates(String search, int page, int size) {
        return PageResponse.from(lots.searchInspectionCandidates(normalize(search), WorkOrderStatus.COMPLETED, pageable(page, size)), LotDtos.ProductResponse::from);
    }
    public PageResponse<QualityDtos.InspectionResponse> search(String search, InspectionJudgement judgement,
        int page, int size, PesUserPrincipal principal) {
        UUID workerId = principal.role() == UserRole.WORKER ? principal.id() : null;
        return PageResponse.from(inspections.search(workerId, normalize(search), judgement, pageable(page, size)), QualityDtos.InspectionResponse::from);
    }
    public QualityDtos.InspectionResponse forLot(UUID lotId, PesUserPrincipal principal) {
        var lot = lots.findDetailById(lotId).orElseThrow(() -> new NotFoundException("생산 LOT를 찾을 수 없습니다."));
        if (principal.role() == UserRole.WORKER && !lot.getProductionResult().getWorkOrder().getAssignedWorker().getId().equals(principal.id())) {
            throw new ForbiddenException("본인 작업지시의 검사 결과만 조회할 수 있습니다.");
        }
        return inspections.findByProductLotId(lotId).map(QualityDtos.InspectionResponse::from).orElse(null);
    }
    @Transactional
    public QualityDtos.InspectionResponse inspect(UUID lotId, QualityDtos.CreateInspectionRequest request, PesUserPrincipal principal) {
        if (principal.role() == UserRole.WORKER) throw new ForbiddenException("관리자와 생산관리자만 검사할 수 있습니다.");
        validate(request);
        var lot = lots.findByIdForUpdate(lotId).orElseThrow(() -> new NotFoundException("생산 LOT를 찾을 수 없습니다."));
        var production = lot.getProductionResult();
        if (production.getWorkOrder().getStatus() != WorkOrderStatus.COMPLETED) {
            throw new ConflictException("작업 완료 후에만 생산 LOT를 검사할 수 있습니다.");
        }
        if (inspections.existsByProductLotId(lotId)) throw new ConflictException("이미 검사한 생산 LOT입니다. 재검사는 지원하지 않습니다.");
        if (request.inspectedQuantity() != production.getProducedQuantity()) {
            throw new InvalidRequestException("전수검사 수량은 해당 생산 LOT의 생산수량과 같아야 합니다.");
        }
        if (request.acceptedQuantity() > production.getGoodQuantity()) {
            throw new InvalidRequestException("생산실적에서 불량으로 기록한 수량을 검사 합격으로 처리할 수 없습니다.");
        }
        var inspector = users.findById(principal.id()).orElseThrow(() -> new NotFoundException("검사자를 찾을 수 없습니다."));
        var result = new InspectionResult(lot, request.inspectedQuantity(), request.acceptedQuantity(), inspector,
            request.note() == null ? null : request.note().trim());
        for (var defect : request.defects()) {
            var code = codes.findById(defect.defectCodeId()).orElseThrow(() -> new NotFoundException("불량코드를 찾을 수 없습니다."));
            result.addDefect(code, defect.quantity());
        }
        return QualityDtos.InspectionResponse.from(inspections.saveAndFlush(result));
    }
    private void validate(QualityDtos.CreateInspectionRequest request) {
        if (request.inspectedQuantity() <= 0 || request.acceptedQuantity() < 0 || request.acceptedQuantity() > request.inspectedQuantity()
            || request.defects() == null || request.defects().size() > 100) {
            throw new InvalidRequestException("검사수량·합격수량·불량내역을 확인해 주세요.");
        }
        long rejected = 0;
        var unique = new HashSet<UUID>();
        for (var defect : request.defects()) {
            if (defect == null || defect.defectCodeId() == null || defect.quantity() <= 0 || !unique.add(defect.defectCodeId())) {
                throw new InvalidRequestException("불량코드는 중복 없이 선택하고 수량은 1 이상이어야 합니다.");
            }
            rejected += defect.quantity();
        }
        if (rejected != (long) request.inspectedQuantity() - request.acceptedQuantity()) {
            throw new InvalidRequestException("검사수량은 합격수량과 불량코드별 수량 합계와 같아야 합니다.");
        }
    }
    private String normalize(String search) { return search == null ? "" : search.trim(); }
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
}
