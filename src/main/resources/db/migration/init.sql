CREATE TABLE orders (
    id              UUID PRIMARY KEY,
    customer_name   VARCHAR(255) NOT NULL,
    customer_email  VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    total_amount    NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);

CREATE TABLE order_items (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name    VARCHAR(255) NOT NULL,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
