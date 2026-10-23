#!/usr/bin/env python3
"""Sync public Blogger RSS/Atom posts into a small, attributed article-card feed.
Only titles, short excerpts, dates, thumbnails and links are republished. Full
article text remains on the original publisher's website.
"""
import html
import json
import re
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

SOURCES = [
    ("வெள்ளிமேடை", "https://vellimedai.blogspot.com/feeds/posts/default"),
    ("வெள்ளிமேடை ப்ளஸ்", "https://vellimedaiplus.blogspot.com/feeds/posts/default"),
    ("சென்னை மாவட்ட ஜமாஅத்துல் உலமா சபை", "https://chennaijamaathululama.blogspot.com/feeds/posts/default"),
]
OUT = Path("article/source-feed.json")
ATOM = "{http://www.w3.org/2005/Atom}"
MEDIA = "{http://search.yahoo.com/mrss/}"
MAX_PAGES_PER_SOURCE = 20
PAGE_SIZE = 100
TIMEOUT = 25

def local_text(node, tag):
    item = node.find(ATOM + tag)
    return "" if item is None else "".join(item.itertext()).strip()

def plain_text(raw):
    raw = re.sub(r"(?is)<(script|style).*?>.*?</\1>", " ", raw or "")
    raw = re.sub(r"(?s)<[^>]+>", " ", raw)
    return html.unescape(re.sub(r"\s+", " ", raw)).strip()

def categorise(text):
    t = text.lower()
    groups = [
        ("குடும்பம்", ["குடும்ப", "திருமணம்", "கணவன்", "மனைவி", "பெற்றோர்", "பிள்ளை", "குழந்தை", "உறவு"]),
        ("குர்ஆன் வாசிப்பு", ["குர்ஆன்", "குரான்", "திருக்குர்ஆன்", "தஃப்ஸீர்", "தப்ஸீர்"]),
        ("நற்பண்புகள்", ["நற்பண்பு", "ஒழுக்கம்", "பொறுமை", "அன்பு", "கோபம்", "அக்லாக்"]),
        ("வாழ்க்கை வழிகாட்டல்", ["வாழ்க்கை", "வாழ்வியல்", "துஆ", "தொழில்", "ஹலால்", "சம்பாத்தியம்"]),
        ("இஸ்லாமிய வரலாறு", ["வரலாறு", "இமாம்", "ஸஹாபா", "சஹாபா", "அஹ்லுல் பைத்", "பாத்திமா"]),
        ("மார்க்க வழிகாட்டல்", ["ஃபிக்ஹ்", "சட்டம்", "ஹதீஸ்", "நோன்பு", "தொழுகை", "ஜகாத்", "பிறை"]),
    ]
    for category, terms in groups:
        if any(term in t for term in terms):
            return category
    return "ஏற்கனவே உள்ள கட்டுரைகள்"

def thumbnail(entry, body):
    thumb = entry.find(MEDIA + "thumbnail")
    if thumb is not None and thumb.attrib.get("url"):
        return thumb.attrib["url"]
    for img in re.findall(r'(?is)<img[^>]+src=["\']([^"\']+)["\']', body or ""):
        if img.startswith("http://") or img.startswith("https://"):
            return img
    return ""

def get_feed(url, start):
    query = urllib.parse.urlencode({"alt": "atom", "max-results": PAGE_SIZE, "start-index": start})
    req = urllib.request.Request(
        url + "?" + query,
        headers={"User-Agent": "YaseenisArticleFeed/1.0 (+https://yaseenis.com/article/index.html)", "Accept": "application/atom+xml, application/xml, text/xml"},
    )
    with urllib.request.urlopen(req, timeout=TIMEOUT) as response:
        return ET.fromstring(response.read())

def fetch_source(source_name, feed_url):
    records = []
    seen = set()
    for page in range(MAX_PAGES_PER_SOURCE):
        start = page * PAGE_SIZE + 1
        try:
            root = get_feed(feed_url, start)
        except Exception as exc:
            print(f"WARNING: feed fetch failed for {source_name} at start-index={start}: {exc}")
            if page == 0:
                raise
            break
        entries = root.findall(ATOM + "entry")
        if not entries:
            break
        for entry in entries:
            title = local_text(entry, "title")
            link = ""
            for candidate in entry.findall(ATOM + "link"):
                if candidate.attrib.get("rel", "alternate") == "alternate" and candidate.attrib.get("href"):
                    link = candidate.attrib["href"]
                    break
            if not title or not link or link in seen:
                continue
            seen.add(link)
            body = local_text(entry, "summary") or local_text(entry, "content")
            excerpt = plain_text(body)
            if len(excerpt) > 260:
                excerpt = excerpt[:257].rsplit(" ", 1)[0] + "…"
            published = local_text(entry, "published") or local_text(entry, "updated")
            categories = [c.attrib.get("term", "") for c in entry.findall(ATOM + "category") if c.attrib.get("term")]
            records.append({
                "title": title,
                "category": categorise(title + " " + excerpt + " " + " ".join(categories)),
                "desc": excerpt or "மூல இணையதளத்தில் முழுக் கட்டுரையையும் வாசிக்கவும்.",
                "sourceName": source_name,
                "sourceUrl": link,
                "published": published,
                "image": thumbnail(entry, body),
                "keywords": " ".join([title, excerpt, *categories, source_name]),
                "path": link,
                "icon": "۞",
                "isSourcePost": True,
            })
        if len(entries) < PAGE_SIZE:
            break
        time.sleep(0.2)
    return records

def main():
    old = []
    if OUT.exists():
        try:
            old = json.loads(OUT.read_text(encoding="utf-8")).get("items", [])
        except Exception:
            old = []
    all_items = []
    success_count = 0
    for name, url in SOURCES:
        try:
            items = fetch_source(name, url)
            all_items.extend(items)
            success_count += 1
            print(f"{name}: {len(items)} posts")
        except Exception as exc:
            print(f"ERROR: {name}: {exc}")
            # Keep the last known cards for a source if its feed is temporarily unavailable.
            all_items.extend(x for x in old if x.get("sourceName") == name)
    if success_count == 0:
        raise SystemExit("All source feeds failed; keeping workflow from overwriting the existing feed.")
    all_items.sort(key=lambda x: x.get("published", ""), reverse=True)
    # Deduplicate URLs across feeds while preserving the newest copy.
    unique = {}
    for item in all_items:
        unique[item["sourceUrl"]] = item
    payload = {
        "updatedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "sourceCount": len(SOURCES),
        "itemCount": len(unique),
        "items": list(unique.values()),
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    new_text = json.dumps(payload, ensure_ascii=False, indent=2) + "\n"
    if not OUT.exists() or OUT.read_text(encoding="utf-8") != new_text:
        OUT.write_text(new_text, encoding="utf-8")
        print(f"Wrote {len(unique)} source cards to {OUT}")
    else:
        print("No source-feed changes.")

if __name__ == "__main__":
    main()
