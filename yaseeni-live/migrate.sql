-- Run this ONCE on an existing yaseeni-live-db before deploying the updated Worker.
-- Existing schema.sql uses CREATE TABLE IF NOT EXISTS, which does not add missing columns.
ALTER TABLE members ADD COLUMN email TEXT;
ALTER TABLE members ADD COLUMN password_salt TEXT;
ALTER TABLE members ADD COLUMN password_hash TEXT;
ALTER TABLE members ADD COLUMN role TEXT NOT NULL DEFAULT 'guest';
ALTER TABLE members ADD COLUMN active INTEGER NOT NULL DEFAULT 1;
CREATE UNIQUE INDEX IF NOT EXISTS idx_members_email ON members(email);
CREATE TABLE IF NOT EXISTS auth_sessions (
 token_hash TEXT PRIMARY KEY,
 member_id TEXT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
 expires_at TEXT NOT NULL,
 created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_auth_sessions_member ON auth_sessions(member_id);
CREATE TABLE IF NOT EXISTS visitor_presence (
 visitor_id TEXT PRIMARY KEY,
 member_id TEXT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
 display_name TEXT NOT NULL,
 city TEXT DEFAULT '',
 country TEXT DEFAULT '',
 first_seen TEXT NOT NULL DEFAULT (datetime('now')),
 last_seen TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_visitor_presence_last_seen ON visitor_presence(last_seen);
