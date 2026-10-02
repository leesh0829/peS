package com.pes.label;

import com.pes.common.persistence.BaseEntity;
import com.pes.lot.domain.ProductLot;
import com.pes.user.domain.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "lot_label_event")
public class LabelEvent extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_lot_id", nullable = false)
    private ProductLot productLot;
    @Column(nullable = false) private int sequenceNumber;
    @Column(nullable = false, length = 30) private String lotNumber;
    @Column(nullable = false, length = 30) private String productCode;
    @Column(nullable = false, length = 100) private String productName;
    @Column(nullable = false) private int producedQuantity;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private UserAccount issuedBy;
    @Column(nullable = false, length = 100) private String issuerName;
    @Column(length = 500) private String reason;
    protected LabelEvent() {}
    public LabelEvent(ProductLot lot, UserAccount user) {
        productLot = lot; sequenceNumber = 1; lotNumber = lot.getLotNumber();
        var result = lot.getProductionResult();
        var product = result.getWorkOrder().getProductionPlan().getProduct();
        productCode = product.getCode(); productName = product.getName(); producedQuantity = result.getProducedQuantity();
        issuedBy = user; issuerName = user.getDisplayName();
    }
    public LabelEvent(LabelEvent original, int sequence, UserAccount user, String reason) {
        productLot = original.productLot; sequenceNumber = sequence; lotNumber = original.lotNumber;
        productCode = original.productCode; productName = original.productName; producedQuantity = original.producedQuantity;
        issuedBy = user; issuerName = user.getDisplayName(); this.reason = reason;
    }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getLotNumber() { return lotNumber; }
    public String getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public int getProducedQuantity() { return producedQuantity; }
    public String getIssuerName() { return issuerName; }
    public String getReason() { return reason; }
}
