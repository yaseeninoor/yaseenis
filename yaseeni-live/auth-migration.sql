-- Run this once in the Yaseeni Live D1 database AFTER schema.sql.
-- Adds email/password login, roles and revocable sessions to existing members.
ALTER TABLE members ADD COLUMN email TEXT;
ALTER TABLE members ADD COLUMN password_salt TEXT;
ALTER TABLE members ADD COLUMN password_hash TEXT;
ALTER TABLE members ADD COLUMN role TEXT NOT NULL DEFAULT 'member';
ALTER TABLE members ADD COLUMN active INTEGER NOT NULL DEFAULT 1;
CREATE UNIQUE INDEX IF NOT EXISTS idx_members_email ON members(email);
CREATE TABLE IF NOT EXISTS auth_sessions (
 token_hash TEXT PRIMARY KEY,
 member_id TEXT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
 expires_at TEXT NOT NULL,
 created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_member ON auth_sessions(member_id);
