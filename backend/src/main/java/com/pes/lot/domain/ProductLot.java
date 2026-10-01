package com.pes.lot.domain;

import com.pes.common.persistence.BaseEntity;
import com.pes.result.domain.ProductionResult;
import jakarta.persistence.*;

@Entity
@Table(name = "product_lot")
public class ProductLot extends BaseEntity {
    @Column(name = "lot_number", nullable = false, unique = true, length = 30)
    private String lotNumber;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "production_result_id", nullable = false, unique = true)
    private ProductionResult productionResult;
    protected ProductLot() {}
    public ProductLot(String lotNumber, ProductionResult productionResult) {
        this.lotNumber = lotNumber;
        this.productionResult = productionResult;
    }
    public String getLotNumber() { return lotNumber; }
    public ProductionResult getProductionResult() { return productionResult; }
}
