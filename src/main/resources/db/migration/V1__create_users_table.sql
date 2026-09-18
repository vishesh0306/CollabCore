CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_admin      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- At most one admin: this unique index only covers rows where is_admin is true,
-- so a second admin row can never be inserted.
CREATE UNIQUE INDEX uq_users_single_admin ON users (is_admin) WHERE is_admin;
