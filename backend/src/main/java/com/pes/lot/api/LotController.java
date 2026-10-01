package com.pes.lot.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.lot.application.LotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api")
public class LotController {
    private final LotService service;
    public LotController(LotService service) { this.service = service; }

    @GetMapping("/material-lots")
    public PageResponse<LotDtos.MaterialResponse> materials(@RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.searchMaterials(search, page, size);
    }
    @PostMapping("/material-lots")
    public ResponseEntity<LotDtos.MaterialResponse> register(@Valid @RequestBody LotDtos.CreateMaterialRequest request) {
        var response = service.registerMaterial(request);
        return ResponseEntity.created(URI.create("/api/material-lots/" + response.id() + "/trace")).body(response);
    }
    @GetMapping("/product-lots")
    public PageResponse<LotDtos.ProductResponse> products(@RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @AuthenticationPrincipal PesUserPrincipal principal) {
        return service.searchProducts(search, page, size, principal);
    }
    @PostMapping("/work-orders/{id}/materials")
    public ResponseEntity<LotDtos.InputResponse> input(@PathVariable UUID id,
        @Valid @RequestBody LotDtos.InputRequest request, @AuthenticationPrincipal PesUserPrincipal principal) {
        return ResponseEntity.status(201).body(service.input(id, request, principal));
    }
    @GetMapping("/work-orders/{id}/materials")
    public List<LotDtos.InputResponse> orderMaterials(@PathVariable UUID id, @AuthenticationPrincipal PesUserPrincipal principal) {
        return service.orderMaterials(id, principal);
    }
    @GetMapping("/product-lots/{id}/trace")
    public LotDtos.ProductTrace productTrace(@PathVariable UUID id, @AuthenticationPrincipal PesUserPrincipal principal) {
        return service.traceProduct(id, principal);
    }
    @GetMapping("/material-lots/{id}/trace")
    public LotDtos.MaterialTrace materialTrace(@PathVariable UUID id, @AuthenticationPrincipal PesUserPrincipal principal) {
        return service.traceMaterial(id, principal);
    }
}
