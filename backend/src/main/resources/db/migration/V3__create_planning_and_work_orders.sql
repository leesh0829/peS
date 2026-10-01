CREATE TABLE production_plan (
    id UUID PRIMARY KEY,
    plan_number VARCHAR(30) NOT NULL,
    product_id UUID NOT NULL,
    due_date DATE NOT NULL,
    target_quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_production_plan_number UNIQUE (plan_number),
    CONSTRAINT fk_production_plan_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_production_plan_target_quantity CHECK (target_quantity > 0),
    CONSTRAINT ck_production_plan_status CHECK (status IN ('DRAFT', 'CONFIRMED'))
);

CREATE INDEX idx_production_plan_status_due_date
    ON production_plan (status, due_date);

CREATE TABLE work_order (
    id UUID PRIMARY KEY,
    work_order_number VARCHAR(30) NOT NULL,
    production_plan_id UUID NOT NULL,
    production_process_id UUID NOT NULL,
    assigned_worker_id UUID NOT NULL,
    target_quantity INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_work_order_number UNIQUE (work_order_number),
    CONSTRAINT fk_work_order_plan FOREIGN KEY (production_plan_id) REFERENCES production_plan (id),
    CONSTRAINT fk_work_order_process FOREIGN KEY (production_process_id) REFERENCES production_process (id),
    CONSTRAINT fk_work_order_worker FOREIGN KEY (assigned_worker_id) REFERENCES app_user (id),
    CONSTRAINT ck_work_order_target_quantity CHECK (target_quantity > 0),
    CONSTRAINT ck_work_order_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED')),
    CONSTRAINT ck_work_order_timestamps CHECK (
        (status = 'WAITING' AND started_at IS NULL AND completed_at IS NULL)
        OR (status = 'IN_PROGRESS' AND started_at IS NOT NULL AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL)
    )
);

CREATE INDEX idx_work_order_plan
    ON work_order (production_plan_id);

CREATE INDEX idx_work_order_assignee_status
    ON work_order (assigned_worker_id, status);

CREATE INDEX idx_work_order_status
    ON work_order (status);
