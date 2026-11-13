PRAGMA foreign_keys = ON;
CREATE TABLE IF NOT EXISTS dhikr (
 id TEXT PRIMARY KEY, name TEXT NOT NULL, arabic TEXT DEFAULT '', tamil TEXT DEFAULT '', english TEXT DEFAULT '',
 active INTEGER NOT NULL DEFAULT 1, sort_order INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS members (
 id TEXT PRIMARY KEY, display_name TEXT NOT NULL, created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS dhikr_totals (
 dhikr_id TEXT PRIMARY KEY REFERENCES dhikr(id), total INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS dhikr_counts (
 dhikr_id TEXT NOT NULL REFERENCES dhikr(id), member_id TEXT NOT NULL REFERENCES members(id),
 total INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL DEFAULT (datetime('now')),
 PRIMARY KEY (dhikr_id, member_id)
);
CREATE TABLE IF NOT EXISTS count_events (
 id INTEGER PRIMARY KEY AUTOINCREMENT, dhikr_id TEXT NOT NULL REFERENCES dhikr(id),
 member_id TEXT NOT NULL REFERENCES members(id), amount INTEGER NOT NULL CHECK(amount > 0),
 created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_count_events_dhikr_date ON count_events(dhikr_id, created_at);
CREATE INDEX IF NOT EXISTS idx_count_events_member ON count_events(member_id, dhikr_id);
INSERT OR IGNORE INTO dhikr (id,name,arabic,tamil,english,sort_order) VALUES
('salawat-main','பொது ஸலவாத்து','اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ','அல்லாஹும்ம ஸல்லி அலா முஹம்மதின்','Allahumma salli ala Muhammad',1),
('salawat-khaleel-awn','யா கலீல் அவ்ன் ஸலவாத்து','يَا خَلِيلَ عَوْن','யா கலீல் அவ்ன்','Ya Khaleel Awn Salawath',2),
('salawat-custom-1','ஆல் வஸ்ஸஹ்பி ஸலவாத்து','اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَالْآلِ وَالصَّحْبِ وَسَلِّمْ','அல்லாஹும்ம ஸல்லி அலா முஹம்மதின் வல் ஆலி வஸ்ஸஹ்பி வஸல்லிம்','Allahumma salli ala Muhammadin wal aali was-sahbi wa sallim',3);
INSERT OR IGNORE INTO dhikr_totals (dhikr_id,total) SELECT id,0 FROM dhikr;