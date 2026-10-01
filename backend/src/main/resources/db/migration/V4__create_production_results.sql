CREATE TABLE production_result (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL,
    produced_quantity INTEGER NOT NULL,
    good_quantity INTEGER NOT NULL,
    defect_quantity INTEGER NOT NULL,
    recorded_by_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_production_result_work_order
        FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_production_result_recorded_by
        FOREIGN KEY (recorded_by_id) REFERENCES app_user (id),
    CONSTRAINT ck_production_result_produced_positive
        CHECK (produced_quantity > 0),
    CONSTRAINT ck_production_result_good_non_negative
        CHECK (good_quantity >= 0),
    CONSTRAINT ck_production_result_defect_non_negative
        CHECK (defect_quantity >= 0),
    CONSTRAINT ck_production_result_quantity_sum
        CHECK (produced_quantity = good_quantity + defect_quantity)
);

CREATE INDEX idx_production_result_work_order_created_at
    ON production_result (work_order_id, created_at DESC);

CREATE INDEX idx_production_result_created_at
    ON production_result (created_at DESC);
