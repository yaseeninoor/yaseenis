CREATE TABLE IF NOT EXISTS members (
  id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL,
  device_id TEXT NOT NULL UNIQUE,
  session_hash TEXT,
  session_expires_at TEXT,
  created_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS daily_counts (
  member_id TEXT NOT NULL REFERENCES members(id) ON DELETE CASCADE,
  count_date TEXT NOT NULL,
  count INTEGER NOT NULL DEFAULT 0 CHECK(count >= 0),
  PRIMARY KEY(member_id, count_date)
);
CREATE INDEX IF NOT EXISTS idx_member_daily_date ON daily_counts(count_date);
CREATE INDEX IF NOT EXISTS idx_member_names ON members(display_name);
