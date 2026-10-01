package com.pes.quality.domain;

import com.pes.common.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "inspection_defect")
public class InspectionDefect extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_result_id", nullable = false)
    private InspectionResult inspectionResult;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "defect_code_id", nullable = false)
    private DefectCode defectCode;
    @Column(nullable = false)
    private int quantity;
    protected InspectionDefect() {}
    public InspectionDefect(InspectionResult inspectionResult, DefectCode code, int quantity) {
        this.inspectionResult = inspectionResult;
        this.defectCode = code;
        this.quantity = quantity;
    }
    public DefectCode getDefectCode() { return defectCode; }
    public int getQuantity() { return quantity; }
}
