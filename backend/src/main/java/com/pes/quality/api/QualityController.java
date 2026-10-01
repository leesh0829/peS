package com.pes.quality.api;

import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.lot.api.LotDtos;
import com.pes.quality.application.QualityService;
import com.pes.quality.domain.InspectionJudgement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api")
public class QualityController {
    private final QualityService service;
    public QualityController(QualityService service) { this.service = service; }
    @GetMapping("/defect-codes")
    public PageResponse<QualityDtos.CodeResponse> codes(@RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.searchCodes(search, page, size);
    }
    @PostMapping("/defect-codes")
    public ResponseEntity<QualityDtos.CodeResponse> createCode(@Valid @RequestBody QualityDtos.CreateCodeRequest request) {
        var response = service.createCode(request);
        return ResponseEntity.created(URI.create("/api/defect-codes?search=" + response.code())).body(response);
    }
    @GetMapping("/inspection-candidates")
    public PageResponse<LotDtos.ProductResponse> candidates(@RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.candidates(search, page, size);
    }
    @GetMapping("/inspections")
    public PageResponse<QualityDtos.InspectionResponse> inspections(@RequestParam(defaultValue = "") String search,
        @RequestParam(required = false) InspectionJudgement judgement,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal PesUserPrincipal principal) {
        return service.search(search, judgement, page, size, principal);
    }
    @GetMapping("/product-lots/{id}/inspection")
    public QualityDtos.LotInspectionResponse forLot(@PathVariable UUID id, @AuthenticationPrincipal PesUserPrincipal principal) {
        return new QualityDtos.LotInspectionResponse(service.forLot(id, principal));
    }
    @PostMapping("/product-lots/{id}/inspect")
    public ResponseEntity<QualityDtos.InspectionResponse> inspect(@PathVariable UUID id,
        @Valid @RequestBody QualityDtos.CreateInspectionRequest request, @AuthenticationPrincipal PesUserPrincipal principal) {
        return ResponseEntity.created(URI.create("/api/product-lots/" + id + "/inspection")).body(service.inspect(id, request, principal));
    }
}
