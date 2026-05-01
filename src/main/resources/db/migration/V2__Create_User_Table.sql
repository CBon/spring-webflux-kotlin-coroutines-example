SET search_path TO application;

CREATE TABLE users
(
    id         SERIAL       NOT NULL PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    name       VARCHAR(255) NOT NULL,
    age        INT          NOT NULL,
    company_id BIGINT       NOT NULL REFERENCES companies (id) ON DELETE CASCADE
);
