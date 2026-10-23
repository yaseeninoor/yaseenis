#!/usr/bin/env python3
"""Dependency-free smoke checks for the Yaseenis static website."""
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[1]
errors = []

required = [
    "index.html", "404.html", "offline.html", "sw.js", "site.webmanifest",
    "robots.txt", "sitemap.xml", "assets/yaseenis-icon.svg",
    "pdfs/index.html", "Quran/Index.html", "hadith/hadith.html",
    "article/index.html", "audio/list.html", "Videos/list.html",
    "search.html", "privacy-policy.html",
]
for name in required:
    if not (ROOT / name).is_file():
        errors.append(f"Missing required file: {name}")

try:
    manifest = json.loads((ROOT / "site.webmanifest").read_text(encoding="utf-8"))
    if manifest.get("start_url") != "/" or manifest.get("scope") != "/":
        errors.append("Manifest start_url and scope should be root-relative.")
    if not manifest.get("name") or not manifest.get("short_name"):
        errors.append("Manifest must define name and short_name.")
    for icon in manifest.get("icons", []):
        icon_path = icon.get("src", "").lstrip("/")
        if icon_path and not (ROOT / icon_path).is_file():
            errors.append(f"Manifest icon does not exist: {icon.get('src')}")
except Exception as exc:
    errors.append(f"Manifest JSON could not be parsed: {exc}")

try:
    tree = ET.parse(ROOT / "sitemap.xml")
    ns = {"sm": "http://www.sitemaps.org/schemas/sitemap/0.9"}
    locations = [node.text for node in tree.findall(".//sm:loc", ns)]
    if not locations:
        errors.append("Sitemap contains no URLs.")
    for location in locations:
        parsed = urlparse(location or "")
        if parsed.scheme != "https" or parsed.netloc != "yaseenis.com":
            errors.append(f"Unexpected sitemap URL: {location}")
            continue
        local_path = parsed.path.lstrip("/") or "index.html"
        if not (ROOT / local_path).is_file():
            errors.append(f"Sitemap URL points to missing file: {location}")
except Exception as exc:
    errors.append(f"Sitemap XML could not be parsed: {exc}")

robots = (ROOT / "robots.txt").read_text(encoding="utf-8")
if "https://yaseenis.com/sitemap.xml" not in robots:
    errors.append("robots.txt must reference the sitemap.")

home = (ROOT / "index.html").read_text(encoding="utf-8")
if 'navigator.serviceWorker.register("/sw.js")' not in home:
    errors.append("Homepage does not register the service worker.")
if 'rel="manifest" href="/site.webmanifest"' not in home:
    errors.append("Homepage does not link to the web manifest.")

for name in ["index.html", "404.html", "offline.html", "pdfs/index.html",
             "Quran/Index.html", "hadith/hadith.html", "article/index.html",
             "audio/list.html", "Videos/list.html"]:
    file = ROOT / name
    if file.is_file():
        data = file.read_text(encoding="utf-8", errors="replace").lower()
        if "<html" not in data or "</html>" not in data:
            errors.append(f"Basic HTML structure check failed: {name}")

if errors:
    print("Yaseenis site smoke checks FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print(f"Yaseenis site smoke checks passed. Validated {len(required)} required files and {len(locations)} sitemap URLs.")
