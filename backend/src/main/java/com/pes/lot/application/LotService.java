package com.pes.lot.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pes.auth.PesUserPrincipal;
import com.pes.common.api.PageResponse;
import com.pes.common.error.*;
import com.pes.lot.api.LotDtos;
import com.pes.lot.domain.*;
import com.pes.user.domain.UserRole;
import com.pes.workorder.domain.*;

@Service
@Transactional(readOnly = true)
public class LotService {
    private final MaterialLotRepository materials;
    private final WorkOrderMaterialRepository inputs;
    private final ProductLotRepository products;
    private final WorkOrderRepository orders;

    public LotService(MaterialLotRepository materials, WorkOrderMaterialRepository inputs,
        ProductLotRepository products, WorkOrderRepository orders) {
        this.materials = materials;
        this.inputs = inputs;
        this.products = products;
        this.orders = orders;
    }
    public PageResponse<LotDtos.MaterialResponse> searchMaterials(String search, int page, int size) {
        return PageResponse.from(materials.search(normalize(search), pageable(page, size)), LotDtos.MaterialResponse::from);
    }
    public PageResponse<LotDtos.ProductResponse> searchProducts(String search, int page, int size, PesUserPrincipal principal) {
        UUID workerId = principal.role() == UserRole.WORKER ? principal.id() : null;
        return PageResponse.from(products.search(workerId, normalize(search), pageable(page, size)), LotDtos.ProductResponse::from);
    }
    @Transactional
    public LotDtos.MaterialResponse registerMaterial(LotDtos.CreateMaterialRequest request) {
        if (request.receivedQuantity() <= 0) throw new InvalidRequestException("등록수량은 1 이상이어야 합니다.");
        String number = "ML-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return LotDtos.MaterialResponse.from(materials.saveAndFlush(new MaterialLot(number,
            request.materialCode(), request.materialName().trim(), request.receivedQuantity())));
    }
    @Transactional
    public LotDtos.InputResponse input(UUID orderId, LotDtos.InputRequest request, PesUserPrincipal principal) {
        if (request.inputQuantity() <= 0) throw new InvalidRequestException("투입수량은 1 이상이어야 합니다.");
        // All commands acquire work-order first, material-lot second; never reverse this order.
        WorkOrder order = orders.findByIdForUpdate(orderId)
            .orElseThrow(() -> new NotFoundException("작업지시를 찾을 수 없습니다."));
        requireOwner(order, principal);
        if (!order.isLotTrackingEnabled() || order.getStatus() != WorkOrderStatus.WAITING) {
            throw new ConflictException("LOT 추적을 사용하는 대기 작업지시에만 자재를 투입할 수 있습니다.");
        }
        MaterialLot material = materials.findByIdForUpdate(request.materialLotId())
            .orElseThrow(() -> new NotFoundException("자재 LOT를 찾을 수 없습니다."));
        if (inputs.existsByWorkOrderIdAndMaterialLotId(orderId, material.getId())) {
            throw new ConflictException("같은 작업지시에 이미 투입한 자재 LOT입니다.");
        }
        if (inputs.sumByWorkOrderId(orderId) + request.inputQuantity() > order.getTargetQuantity()) {
            throw new ConflictException("자재 투입 합계는 작업지시 목표수량을 초과할 수 없습니다.");
        }
        if (inputs.sumByMaterialLotId(material.getId()) + request.inputQuantity() > material.getReceivedQuantity()) {
            throw new ConflictException("자재 LOT의 등록수량을 초과해 투입할 수 없습니다.");
        }
        return LotDtos.InputResponse.from(inputs.saveAndFlush(new WorkOrderMaterial(order, material, request.inputQuantity())));
    }
    public java.util.List<LotDtos.InputResponse> orderMaterials(UUID orderId, PesUserPrincipal principal) {
        WorkOrder order = orders.findById(orderId).orElseThrow(() -> new NotFoundException("작업지시를 찾을 수 없습니다."));
        requireVisible(order, principal);
        return inputs.findByWorkOrderIdOrderByCreatedAtAsc(orderId).stream().map(LotDtos.InputResponse::from).toList();
    }
    public LotDtos.ProductTrace traceProduct(UUID id, PesUserPrincipal principal) {
        ProductLot lot = products.findDetailById(id).orElseThrow(() -> new NotFoundException("생산 LOT를 찾을 수 없습니다."));
        WorkOrder order = lot.getProductionResult().getWorkOrder();
        requireVisible(order, principal);
        return new LotDtos.ProductTrace(LotDtos.ProductResponse.from(lot),
            inputs.findByWorkOrderIdOrderByCreatedAtAsc(order.getId()).stream().map(LotDtos.InputResponse::from).toList());
    }
    public LotDtos.MaterialTrace traceMaterial(UUID id, PesUserPrincipal principal) {
        MaterialLot lot = materials.findById(id).orElseThrow(() -> new NotFoundException("자재 LOT를 찾을 수 없습니다."));
        var visible = inputs.findByMaterialLotIdOrderByCreatedAtAsc(id).stream()
            .filter(input -> principal.role() != UserRole.WORKER || input.getWorkOrder().getAssignedWorker().getId().equals(principal.id()))
            .toList();
        var productLots = visible.stream().flatMap(input -> products.findByProductionResultWorkOrderIdOrderByCreatedAtAsc(input.getWorkOrder().getId()).stream())
            .map(LotDtos.ProductResponse::from).toList();
        return new LotDtos.MaterialTrace(LotDtos.MaterialResponse.from(lot), visible.stream().map(LotDtos.InputResponse::from).toList(), productLots);
    }
    private void requireOwner(WorkOrder order, PesUserPrincipal principal) {
        if (principal.role() != UserRole.WORKER || !order.getAssignedWorker().getId().equals(principal.id())) {
            throw new ForbiddenException("배정된 작업자만 자재를 투입할 수 있습니다.");
        }
    }
    private void requireVisible(WorkOrder order, PesUserPrincipal principal) {
        if (principal.role() == UserRole.WORKER) requireOwner(order, principal);
    }
    private String normalize(String search) { return search == null ? "" : search.trim(); }
    private PageRequest pageable(int page, int size) {
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }
}
