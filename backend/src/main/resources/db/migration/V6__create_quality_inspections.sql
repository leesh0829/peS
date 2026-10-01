CREATE TABLE defect_code (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE inspection_result (
    id UUID PRIMARY KEY,
    product_lot_id UUID NOT NULL UNIQUE REFERENCES product_lot(id),
    inspected_quantity INTEGER NOT NULL CHECK (inspected_quantity > 0),
    accepted_quantity INTEGER NOT NULL CHECK (accepted_quantity >= 0),
    rejected_quantity INTEGER NOT NULL CHECK (rejected_quantity >= 0),
    judgement VARCHAR(10) NOT NULL CHECK (judgement IN ('PASS', 'FAIL')),
    inspected_by_id UUID NOT NULL REFERENCES app_user(id),
    note VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_inspection_sum CHECK (inspected_quantity::BIGINT = accepted_quantity::BIGINT + rejected_quantity),
    CONSTRAINT ck_inspection_judgement CHECK ((judgement = 'PASS' AND rejected_quantity = 0)
        OR (judgement = 'FAIL' AND rejected_quantity > 0))
);
CREATE TABLE inspection_defect (
    id UUID PRIMARY KEY,
    inspection_result_id UUID NOT NULL REFERENCES inspection_result(id),
    defect_code_id UUID NOT NULL REFERENCES defect_code(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inspection_defect UNIQUE (inspection_result_id, defect_code_id)
);
CREATE INDEX idx_inspection_created ON inspection_result(created_at DESC);
