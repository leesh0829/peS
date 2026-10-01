package com.pes.quality.domain;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize;
import com.pes.common.persistence.BaseEntity;
import com.pes.lot.domain.ProductLot;
import com.pes.user.domain.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "inspection_result")
public class InspectionResult extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_lot_id", nullable = false, unique = true)
    private ProductLot productLot;
    @Column(name = "inspected_quantity", nullable = false)
    private int inspectedQuantity;
    @Column(name = "accepted_quantity", nullable = false)
    private int acceptedQuantity;
    @Column(name = "rejected_quantity", nullable = false)
    private int rejectedQuantity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InspectionJudgement judgement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspected_by_id", nullable = false)
    private UserAccount inspectedBy;
    @Column(length = 500)
    private String note;
    @OneToMany(mappedBy = "inspectionResult", cascade = CascadeType.PERSIST)
    @BatchSize(size = 100)
    @OrderBy("createdAt ASC, id ASC")
    private List<InspectionDefect> defects = new ArrayList<>();

    protected InspectionResult() {}
    public InspectionResult(ProductLot lot, int inspected, int accepted, UserAccount inspector, String note) {
        this.productLot = lot;
        this.inspectedQuantity = inspected;
        this.acceptedQuantity = accepted;
        this.rejectedQuantity = inspected - accepted;
        this.judgement = rejectedQuantity == 0 ? InspectionJudgement.PASS : InspectionJudgement.FAIL;
        this.inspectedBy = inspector;
        this.note = note;
    }
    public void addDefect(DefectCode code, int quantity) { defects.add(new InspectionDefect(this, code, quantity)); }
    public ProductLot getProductLot() { return productLot; }
    public int getInspectedQuantity() { return inspectedQuantity; }
    public int getAcceptedQuantity() { return acceptedQuantity; }
    public int getRejectedQuantity() { return rejectedQuantity; }
    public InspectionJudgement getJudgement() { return judgement; }
    public UserAccount getInspectedBy() { return inspectedBy; }
    public String getNote() { return note; }
    public List<InspectionDefect> getDefects() { return List.copyOf(defects); }
}
