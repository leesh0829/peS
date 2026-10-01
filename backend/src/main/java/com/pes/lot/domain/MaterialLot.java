package com.pes.lot.domain;

import com.pes.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "material_lot")
public class MaterialLot extends BaseEntity {
    @Column(name = "lot_number", nullable = false, unique = true, length = 30)
    private String lotNumber;
    @Column(name = "material_code", nullable = false, length = 30)
    private String materialCode;
    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;
    @Column(name = "received_quantity", nullable = false)
    private int receivedQuantity;

    protected MaterialLot() {}
    public MaterialLot(String lotNumber, String materialCode, String materialName, int receivedQuantity) {
        this.lotNumber = lotNumber;
        this.materialCode = materialCode;
        this.materialName = materialName;
        this.receivedQuantity = receivedQuantity;
    }
    public String getLotNumber() { return lotNumber; }
    public String getMaterialCode() { return materialCode; }
    public String getMaterialName() { return materialName; }
    public int getReceivedQuantity() { return receivedQuantity; }
}
