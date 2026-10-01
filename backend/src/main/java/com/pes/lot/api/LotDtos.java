package com.pes.lot.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.pes.lot.domain.*;
import com.pes.result.api.ProductionResultDtos;
import com.pes.workorder.domain.WorkOrderStatus;
import jakarta.validation.constraints.*;

public final class LotDtos {
    private LotDtos() {}
    public record CreateMaterialRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_-]{2,30}$") String materialCode,
        @NotBlank @Size(max = 100) String materialName,
        @Min(1) int receivedQuantity) {}
    public record InputRequest(@NotNull UUID materialLotId, @Min(1) int inputQuantity) {}
    public record MaterialResponse(UUID id, String lotNumber, String materialCode, String materialName,
        int receivedQuantity, Instant createdAt) {
        public static MaterialResponse from(MaterialLot lot) {
            return new MaterialResponse(lot.getId(), lot.getLotNumber(), lot.getMaterialCode(),
                lot.getMaterialName(), lot.getReceivedQuantity(), lot.getCreatedAt());
        }
    }
    public record InputResponse(UUID id, UUID workOrderId, String workOrderNumber, WorkOrderStatus status,
        MaterialResponse materialLot, int inputQuantity, String recordedBy, Instant createdAt) {
        public static InputResponse from(WorkOrderMaterial input) {
            var order = input.getWorkOrder();
            return new InputResponse(input.getId(), order.getId(), order.getWorkOrderNumber(), order.getStatus(),
                MaterialResponse.from(input.getMaterialLot()), input.getInputQuantity(),
                input.getRecordedBy().getDisplayName(), input.getCreatedAt());
        }
    }
    public record ProductResponse(UUID id, String lotNumber, ProductionResultDtos.Response result) {
        public static ProductResponse from(ProductLot lot) {
            return new ProductResponse(lot.getId(), lot.getLotNumber(), ProductionResultDtos.Response.from(lot.getProductionResult()));
        }
    }
    public record ProductTrace(ProductResponse productLot, List<InputResponse> materials) {}
    public record MaterialTrace(MaterialResponse materialLot, List<InputResponse> inputs, List<ProductResponse> productLots) {}
}
