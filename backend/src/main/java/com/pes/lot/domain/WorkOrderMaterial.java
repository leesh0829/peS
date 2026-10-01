package com.pes.lot.domain;

import com.pes.common.persistence.BaseEntity;
import com.pes.user.domain.UserAccount;
import com.pes.workorder.domain.WorkOrder;
import jakarta.persistence.*;

@Entity
@Table(name = "work_order_material")
public class WorkOrderMaterial extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_lot_id", nullable = false)
    private MaterialLot materialLot;
    @Column(name = "input_quantity", nullable = false)
    private int inputQuantity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private UserAccount recordedBy;
    protected WorkOrderMaterial() {}
    public WorkOrderMaterial(WorkOrder workOrder, MaterialLot materialLot, int inputQuantity) {
        this.workOrder = workOrder;
        this.materialLot = materialLot;
        this.inputQuantity = inputQuantity;
        this.recordedBy = workOrder.getAssignedWorker();
    }
    public WorkOrder getWorkOrder() { return workOrder; }
    public MaterialLot getMaterialLot() { return materialLot; }
    public int getInputQuantity() { return inputQuantity; }
    public UserAccount getRecordedBy() { return recordedBy; }
}
