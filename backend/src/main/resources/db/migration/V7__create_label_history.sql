CREATE TABLE lot_label_event (
    id UUID PRIMARY KEY,
    product_lot_id UUID NOT NULL REFERENCES product_lot(id),
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    lot_number VARCHAR(30) NOT NULL,
    product_code VARCHAR(30) NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    produced_quantity INTEGER NOT NULL CHECK (produced_quantity > 0),
    issued_by UUID NOT NULL REFERENCES app_user(id),
    issuer_name VARCHAR(100) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (product_lot_id, sequence_number),
    CHECK ((sequence_number = 1 AND reason IS NULL) OR (sequence_number > 1 AND length(trim(reason)) > 0 AND reason IS NOT NULL))
);
