#!/bin/bash

set -e

psql -ev ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOF
	create user "$APP_POSTGRES_USER" with password '$APP_POSTGRES_PASSWORD' CREATEDB;
	create database "$APP_POSTGRES_DATABASE" owner "$APP_POSTGRES_USER";
	revoke all on database "$APP_POSTGRES_DATABASE" from public;
	EOF
