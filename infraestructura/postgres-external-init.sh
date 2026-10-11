#!/usr/bin/env bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<'SQL'
\getenv phone_password PHONE_DB_PASSWORD
\getenv payment_password PAYMENT_DB_PASSWORD
CREATE ROLE phone_reader LOGIN PASSWORD :'phone_password';
CREATE ROLE payments LOGIN PASSWORD :'payment_password';
CREATE DATABASE phone_identity;
CREATE DATABASE payments OWNER payments;
REVOKE CONNECT ON DATABASE phone_identity FROM PUBLIC;
GRANT CONNECT ON DATABASE phone_identity TO phone_reader;
REVOKE CONNECT ON DATABASE payments FROM PUBLIC;
GRANT CONNECT ON DATABASE payments TO payments;
SQL
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname phone_identity -f /datasets/phone-seed.sql
