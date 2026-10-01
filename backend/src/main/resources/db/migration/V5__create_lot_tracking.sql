ALTER TABLE work_order ADD COLUMN lot_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE material_lot (
    id UUID PRIMARY KEY,
    lot_number VARCHAR(30) NOT NULL UNIQUE,
    material_code VARCHAR(30) NOT NULL,
    material_name VARCHAR(100) NOT NULL,
    received_quantity INTEGER NOT NULL CHECK (received_quantity > 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE work_order_material (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL REFERENCES work_order(id),
    material_lot_id UUID NOT NULL REFERENCES material_lot(id),
    input_quantity INTEGER NOT NULL CHECK (input_quantity > 0),
    recorded_by_id UUID NOT NULL REFERENCES app_user(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_work_order_material UNIQUE (work_order_id, material_lot_id)
);
CREATE INDEX idx_work_order_material_lot ON work_order_material(material_lot_id);

CREATE TABLE product_lot (
    id UUID PRIMARY KEY,
    lot_number VARCHAR(30) NOT NULL UNIQUE,
    production_result_id UUID NOT NULL UNIQUE REFERENCES production_result(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_product_lot_created ON product_lot(created_at DESC);
CREATE INDEX idx_material_lot_created ON material_lot(created_at DESC);
