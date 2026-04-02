ALTER TABLE users
    ADD COLUMN email         VARCHAR(255) NULL,
    ADD COLUMN refresh_token VARCHAR(512) NULL,
    ADD CONSTRAINT uq_users_email    UNIQUE (email),
    ADD CONSTRAINT uq_users_username UNIQUE (username);
