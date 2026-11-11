CREATE TABLE IF NOT EXISTS salawath_presence (
  visitor_id TEXT PRIMARY KEY,
  display_name TEXT NOT NULL DEFAULT '',
  first_seen TEXT NOT NULL,
  last_seen TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_salawath_presence_last_seen ON salawath_presence(last_seen);
CREATE TABLE IF NOT EXISTS salawath_events (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  visitor_id TEXT NOT NULL,
  amount INTEGER NOT NULL CHECK(amount > 0),
  event_date TEXT NOT NULL,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_salawath_events_date ON salawath_events(event_date);
CREATE INDEX IF NOT EXISTS idx_salawath_events_visitor_date ON salawath_events(visitor_id, event_date);
CREATE INDEX IF NOT EXISTS idx_salawath_events_date_created ON salawath_events(event_date, created_at);

-- Approximate city-level visitor locations; no GPS coordinates are stored.
CREATE TABLE IF NOT EXISTS salawath_visitor_locations (
  visitor_id TEXT PRIMARY KEY,
  city TEXT NOT NULL DEFAULT '',
  region TEXT NOT NULL DEFAULT '',
  country TEXT NOT NULL DEFAULT '',
  updated_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_salawath_locations_city ON salawath_visitor_locations(city, country);
