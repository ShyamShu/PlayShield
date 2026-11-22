#!/bin/sh
# Runs once, on first startup with an empty data directory.
# One database + one owner role per service; no cross-service access.
set -eu

create_db() {
  db="$1"; user="$2"; pass="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<EOSQL
CREATE ROLE ${user} LOGIN PASSWORD '${pass}';
CREATE DATABASE ${db} OWNER ${user};
REVOKE ALL ON DATABASE ${db} FROM PUBLIC;
EOSQL
}

create_db identity_db       identity_user       "$IDENTITY_DB_PASSWORD"
create_db transaction_db    transaction_user    "$TRANSACTION_DB_PASSWORD"
create_db risk_db           risk_user           "$RISK_DB_PASSWORD"
create_db investigation_db  investigation_user  "$INVESTIGATION_DB_PASSWORD"