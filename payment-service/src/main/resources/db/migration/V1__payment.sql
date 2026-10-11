CREATE TABLE payment (
 id UUID PRIMARY KEY,
 owner VARCHAR(255) NOT NULL,
 idempotency_key VARCHAR(80) NOT NULL,
 amount NUMERIC(9,2) NOT NULL CHECK (amount > 0),
 currency VARCHAR(3) NOT NULL CHECK (currency IN ('PEN','USD')),
 status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','APPROVED')),
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(owner,idempotency_key)
);
CREATE INDEX payment_owner_idx ON payment(owner);
