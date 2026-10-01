package com.pes.quality.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.pes.lot.api.LotDtos;
import com.pes.quality.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class QualityDtos {
    private QualityDtos() {}
    public record CreateCodeRequest(@NotBlank @Pattern(regexp = "^[A-Z0-9_-]{2,30}$") String code,
        @NotBlank @Size(max = 100) String name) {}
    public record CodeResponse(UUID id, String code, String name) {
        public static CodeResponse from(DefectCode code) { return new CodeResponse(code.getId(), code.getCode(), code.getName()); }
    }
    public record DefectRequest(@NotNull UUID defectCodeId, @Min(1) int quantity) {}
    public record CreateInspectionRequest(@Min(1) int inspectedQuantity, @Min(0) int acceptedQuantity,
        @NotNull @Size(max = 100) List<@NotNull @Valid DefectRequest> defects, @Size(max = 500) String note) {}
    public record DefectResponse(CodeResponse defectCode, int quantity) {}
    public record Inspector(UUID id, String displayName) {}
    public record LotInspectionResponse(InspectionResponse inspection) {}
    public record InspectionResponse(UUID id, LotDtos.ProductResponse productLot, int inspectedQuantity,
        int acceptedQuantity, int rejectedQuantity, InspectionJudgement judgement, List<DefectResponse> defects,
        Inspector inspectedBy, Instant inspectedAt, String note) {
        public static InspectionResponse from(InspectionResult result) {
            var inspector = result.getInspectedBy();
            return new InspectionResponse(result.getId(), LotDtos.ProductResponse.from(result.getProductLot()),
                result.getInspectedQuantity(), result.getAcceptedQuantity(), result.getRejectedQuantity(), result.getJudgement(),
                result.getDefects().stream().map(defect -> new DefectResponse(CodeResponse.from(defect.getDefectCode()), defect.getQuantity())).toList(),
                new Inspector(inspector.getId(), inspector.getDisplayName()), result.getCreatedAt(), result.getNote());
        }
    }
}
