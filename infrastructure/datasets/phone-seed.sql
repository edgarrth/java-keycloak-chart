CREATE TABLE IF NOT EXISTS phone_identity (
 phone VARCHAR(12) PRIMARY KEY CHECK (phone ~ '^\+51[0-9]{9}$'),
 first_name VARCHAR(80) NOT NULL, last_name VARCHAR(80) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
 salt BYTEA NOT NULL, password_hash BYTEA NOT NULL,
 iterations INTEGER NOT NULL CHECK(iterations >= 600000), enabled BOOLEAN NOT NULL DEFAULT true
);
INSERT INTO phone_identity(phone,first_name,last_name,email,salt,password_hash,iterations,enabled) VALUES
 ('+51987654321','Comerciante','Axiz','phone-51987654321@example.test',decode('4b23f59548622d8900c1e1576ffe827a','hex'),decode('5b288a71a429d766db270d5a00f16d1328613d752e4b2e87b25c4c17773c8697','hex'),600000,true),
 ('+51912345678','Supervisor','Axiz','phone-51912345678@example.test',decode('0b1445ae45cc76c47bb9d4e572e3a556','hex'),decode('fc05ad4158397b48a5765654cb79e364f776f3907e82776d7240e49af10e30b2','hex'),600000,true),
 ('+51911111111','Deshabilitado','Axiz','phone-51911111111@example.test',decode('004820799a264bf971c3efebf9cf5d28','hex'),decode('344c1d3e41238e5fb2d8da649aae26ed8d9be9079240c8e1448f7cc183202f16','hex'),600000,false)
 ON CONFLICT(phone) DO NOTHING;
GRANT SELECT ON phone_identity TO phone_reader;
