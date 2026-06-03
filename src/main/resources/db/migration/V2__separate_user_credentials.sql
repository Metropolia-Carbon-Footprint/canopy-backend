-- Move local email + password authentication data out of the users table.
-- This keeps users independent of the authentication method and allows
-- Microsoft Entra identities to be linked later without changing trip ownership.

CREATE TABLE local_credentials (
    user_id INTEGER PRIMARY KEY
        REFERENCES users(user_id) ON DELETE CASCADE,

    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Preserve any existing users and their password hashes.
INSERT INTO local_credentials (user_id, password_hash)
SELECT user_id, password_hash
FROM users;

ALTER TABLE users
DROP COLUMN password_hash;

-- A newly registered user should always receive the normal user role
-- unless an administrator explicitly changes it.
ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'USER';

ALTER TABLE users
    ADD CONSTRAINT users_role_check
        CHECK (role IN ('USER', 'ADMIN'));

-- The original UNIQUE constraint is case-sensitive in PostgreSQL.
-- Replace it so that Example@metropolia.fi and example@metropolia.fi
-- cannot become separate accounts.
ALTER TABLE users
DROP CONSTRAINT users_email_key;

CREATE UNIQUE INDEX users_email_lower_unique
    ON users (LOWER(email));

-- External identities are not required for the initial local login,
-- but this table prepares the database for Microsoft school accounts.
CREATE TABLE user_identities (
    user_identity_id SERIAL PRIMARY KEY,

    user_id INTEGER NOT NULL
        REFERENCES users(user_id) ON DELETE CASCADE,

    provider VARCHAR(50) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (provider, issuer, subject)
);