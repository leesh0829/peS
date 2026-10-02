package com.pes.label;

import java.net.URI;
import java.util.UUID;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-lots/{id}/labels")
public class LabelController {
    public record ReprintRequest(@NotBlank @Size(max = 500) String reason) {}
    private final LabelService service;
    public LabelController(LabelService service) { this.service = service; }
    @GetMapping
    public PageResponse<LabelService.Response> history(@PathVariable UUID id, @RequestParam(defaultValue = "0") @Min(0) int page,
        @AuthenticationPrincipal PesUserPrincipal principal) { return service.history(id, page, principal); }
    @PostMapping("/issue")
    public ResponseEntity<LabelService.Response> issue(@PathVariable UUID id, @AuthenticationPrincipal PesUserPrincipal principal) {
        return ResponseEntity.created(URI.create("/api/product-lots/" + id + "/labels")).body(service.issue(id, false, null, principal));
    }
    @PostMapping("/reprint")
    public ResponseEntity<LabelService.Response> reprint(@PathVariable UUID id, @Valid @RequestBody ReprintRequest request,
        @AuthenticationPrincipal PesUserPrincipal principal) {
        return ResponseEntity.created(URI.create("/api/product-lots/" + id + "/labels")).body(service.issue(id, true, request.reason(), principal));
    }
}
