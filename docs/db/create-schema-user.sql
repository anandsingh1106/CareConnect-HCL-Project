-- Run with psql as a PostgreSQL administrator.
CREATE ROLE careconnect LOGIN PASSWORD 'careconnect';
CREATE DATABASE careconnect OWNER careconnect;
