CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    github_token VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS standups (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    standup_text TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    commit_count INTEGER NOT NULL,
    pr_count INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_standups_username_generated_at
    ON standups (username, generated_at DESC);
