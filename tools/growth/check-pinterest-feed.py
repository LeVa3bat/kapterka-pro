"""Check the published RSS artifact with a standard XML parser; never post anything."""
import json
import re
import struct
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from email.utils import parsedate_to_datetime
from pathlib import Path
from urllib.parse import parse_qs, urlparse

ROOT = Path(__file__).resolve().parents[2]
PACK = ROOT / "growth/social-channels-2026-10-14/pinterest"
ALLOWED_PAGES = {
    "uchet-instrumenta-android.html", "inventarizaciya-na-android.html",
    "peremeshchenie-mezhdu-skladami-android.html", "uchet-vydachi-imushchestva-android.html",
    "zhurnal-dvizheniya-imushchestva-android.html", "uchet-ostatkov-na-telefone.html",
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def jpeg_size(data):
    require(data[:2] == b"\xff\xd8", "Cover must be an actual JPEG")
    offset = 2
    while offset < len(data):
        require(data[offset] == 255, "Malformed JPEG segment")
        while data[offset] == 255:
            offset += 1
        marker = data[offset]
        offset += 1
        if marker in (0xD8, 0xD9) or 0xD0 <= marker <= 0xD7:
            continue
        require(marker != 0xDA, "JPEG dimensions not found before image data")
        length = struct.unpack_from(">H", data, offset)[0]
        require(length >= 2 and offset + length <= len(data), "Invalid JPEG segment length")
        if marker in (0xC0, 0xC1, 0xC2):
            height, width = struct.unpack_from(">HH", data, offset + 3)
            return width, height
        offset += length
    raise ValueError("Missing JPEG dimensions")


def check():
    catalog = json.loads((PACK / "catalog.json").read_text(encoding="utf-8"))
    state = json.loads((PACK / "publication-state.json").read_text(encoding="utf-8"))
    entries = catalog["items"]
    require(len(entries) == 6, "Expected six curated materials, not a site-history import")
    for field in ("slug", "page", "guid", "image"):
        require(len({item[field] for item in entries}) == len(entries), "Duplicate catalog " + field)
    require({item["page"] for item in entries} == ALLOWED_PAGES, "Unexpected or missing useful page")
    by_slug = {item["slug"]: item for item in entries}
    active = state["activeSlugs"]
    require(bool(active) and len(set(active)) == len(active), "Empty or duplicate activeSlugs")
    require(set(active) <= set(by_slug), "Unknown active item")
    if state["stage"] == "pilot":
        require(active == ["tool-accounting-templates"], "Pilot must contain only the initial template instruction")
    else:
        require(state["stage"] == "expanded", "Unknown publication stage")
        proof = state["pilotVerification"]
        pin = urlparse(proof["pinUrl"] or "")
        require(pin.scheme == "https" and pin.hostname in ("pinterest.com", "www.pinterest.com")
                and re.fullmatch(r"/pin/\d+/?", pin.path), "Expansion needs an actual Pinterest pin URL")
        datetime.fromisoformat(proof["checkedAt"].replace("Z", "+00:00"))
        for key in ("imageMatchesEnclosure", "destinationWorksAndRetainsUtm", "duplicateCheckPassed"):
            require(proof[key] is True, "Expansion needs verified " + key)
    for entry in entries:
        canonical = "https://kapterka-pro.ru/" + entry["page"]
        require(entry["guid"] == canonical, "GUID must stay canonical without UTM")
        require((ROOT / "docs" / entry["page"]).is_file(), "Missing target article")
        link = urlparse(entry["link"])
        require(link.scheme == "https" and link.netloc == "kapterka-pro.ru"
                and link.path == "/" + entry["page"] and not link.fragment, "Wrong article destination")
        require(parse_qs(link.query) == {"utm_source": ["pinterest"], "utm_medium": ["organic_social"],
                "utm_campaign": ["evergreen"], "utm_content": [entry["slug"]]}, "Wrong or duplicated UTM")
        require(entry["image"] == "https://kapterka-pro.ru/images/pinterest/" + entry["slug"] + ".jpg", "Wrong cover URL")
        cover = ROOT / "docs/images/pinterest" / (entry["slug"] + ".jpg")
        require(jpeg_size(cover.read_bytes()) == (1000, 1500), "Cover dimensions must be 1000 x 1500")
        datetime.fromisoformat(entry["feedPublishedAt"])
        require(entry["title"].strip() and entry["description"].strip(), "Empty text")
    rss_bytes = (ROOT / "docs/pinterest.xml").read_bytes()
    require(b"<!DOCTYPE" not in rss_bytes and b"<!ENTITY" not in rss_bytes, "RSS must not use external entities")
    rss = ET.fromstring(rss_bytes)
    require(rss.tag == "rss" and rss.get("version") == "2.0", "Expected RSS 2.0, not Atom")
    channels = rss.findall("channel")
    require(len(channels) == 1, "Expected one channel")
    items = channels[0].findall("item")
    require(len(items) == len(active), "Serialized item count differs from activeSlugs")
    expected = {by_slug[slug]["guid"]: by_slug[slug] for slug in active}
    require({item.findtext("guid") for item in items} == set(expected), "Unexpected or duplicate RSS GUID")
    for item in items:
        entry = expected[item.findtext("guid")]
        for tag in ("title", "description", "link", "guid"):
            require(len(item.findall(tag)) == 1 and item.findtext(tag) == entry[tag], "Serialized " + tag + " mismatch")
        date = parsedate_to_datetime(item.findtext("pubDate"))
        require(date == datetime.fromisoformat(entry["feedPublishedAt"]), "Unstable or incorrect RSS pubDate")
        enclosures = item.findall("enclosure")
        require(len(enclosures) == 1, "Exactly one enclosure per item is required")
        enclosure = enclosures[0]
        require(enclosure.get("url") == entry["image"] and enclosure.get("type") == "image/jpeg", "Wrong enclosure")
        require(int(enclosure.get("length")) == (ROOT / "docs/images/pinterest" / (entry["slug"] + ".jpg")).stat().st_size, "Enclosure byte length mismatch")
        require(not re.search(r"<\s*img\b", item.findtext("description"), re.I), "Description must not add a second image")
        for element in item.iter():
            local_name = element.tag.rsplit("}", 1)[-1]
            require(local_name not in ("image", "content", "thumbnail"), "Do not add competing image sources")
    print(f"PASS: valid RSS XML, {len(items)} active item(s), six unique vertical covers, stable GUID/date, one enclosure, UTM and activation gate")


if __name__ == "__main__":
    try:
        check()
    except (KeyError, TypeError, ValueError, OSError, ET.ParseError, struct.error, IndexError) as error:
        print("FAIL:", error, file=sys.stderr)
        sys.exit(1)
