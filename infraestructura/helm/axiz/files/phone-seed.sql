CREATE TABLE IF NOT EXISTS phone_identity (
 phone VARCHAR(12) PRIMARY KEY CHECK (phone ~ '^\+51[0-9]{9}$'),
 first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
 salt BYTEA NOT NULL, password_hash BYTEA NOT NULL,
 iterations INTEGER NOT NULL CHECK(iterations >= 600000), enabled BOOLEAN NOT NULL DEFAULT true
);
INSERT INTO phone_identity(phone,first_name,last_name,email,salt,password_hash,iterations) VALUES
 ('+51987654321','Comerciante','Axiz','merchant-phone@example.test',decode('9c4e19fe6f22a0d92b730a9f5680e17c','hex'),decode('0d84daf6d320d655c7772e97c96aaa1e1823eecfecba10ccfad8fc242328cdf2','hex'),600000)
 ON CONFLICT(phone) DO NOTHING;
GRANT SELECT ON phone_identity TO phone_reader;
