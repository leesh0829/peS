package com.pes.quality.domain;

import com.pes.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@org.hibernate.annotations.BatchSize(size = 100)
@Table(name = "defect_code")
public class DefectCode extends BaseEntity {
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    protected DefectCode() {}
    public DefectCode(String code, String name) { this.code = code; this.name = name; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
