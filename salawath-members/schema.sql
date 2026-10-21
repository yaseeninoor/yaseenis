CREATE TABLE IF NOT EXISTS otp_requests (
  phone_e164 TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  code_hash TEXT NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  expires_at TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  phone_e164 TEXT NOT NULL UNIQUE,
  session_hash TEXT,
  session_expires_at TEXT,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS daily_counts (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  count_date TEXT NOT NULL,
  count INTEGER NOT NULL DEFAULT 0 CHECK(count >= 0),
  PRIMARY KEY(user_id, count_date)
);
CREATE INDEX IF NOT EXISTS idx_daily_counts_date ON daily_counts(count_date);
CREATE INDEX IF NOT EXISTS idx_users_name ON users(display_name);
