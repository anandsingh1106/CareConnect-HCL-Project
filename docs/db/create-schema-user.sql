-- Run as SYSDBA:  sqlplus "/ as sysdba" @create-schema-user.sql
-- Drops and recreates the CARECONNECT schema. The backend (Liquibase) then creates all
-- tables, sequences, constraints, indexes and seed data on its next start.
WHENEVER SQLERROR CONTINUE
DROP USER careconnect CASCADE;
WHENEVER SQLERROR EXIT FAILURE
CREATE USER careconnect IDENTIFIED BY careconnect DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW, CREATE PROCEDURE, CREATE TRIGGER TO careconnect;
EXIT
