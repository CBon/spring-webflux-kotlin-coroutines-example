CREATE SCHEMA IF NOT EXISTS application;
SET search_path TO application;

CREATE TABLE companies
(
    id      SERIAL       NOT NULL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL
);
