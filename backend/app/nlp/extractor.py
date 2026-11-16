from __future__ import annotations

import re
from typing import Any

BRAND_PATTERNS = {
    "Samsung": [r"\bsamsung\b", r"\bgalaxy\b", r"স্যামসাং"],
    "Apple": [r"\biphone\b", r"\bapple\b", r"আইফোন"],
    "Xiaomi": [r"\bxiaomi\b", r"\bredmi\b", r"\bpoco\b", r"শাওমি", r"রেডমি"],
    "Realme": [r"\brealme\b", r"রিয়েলমি"],
    "Vivo": [r"\bvivo\b", r"ভিভো"],
    "Oppo": [r"\boppo\b", r"অপো"],
    "OnePlus": [r"\boneplus\b", r"ওয়ানপ্লাস"],
    "Tecno": [r"\btecno\b", r"টেকনো"],
    "Symphony": [r"\bsymphony\b", r"সিম্ফনি"],
    "Walton": [r"\bwalton\b", r"ওয়ালটন"],
}

DHAKA_AND_BD_LOCATIONS = [
    ("Farmgate Bus Stand", [r"farmgate(?:\s+bus\s+stand)?", r"ফার্মগেট"]),
    ("Mirpur 10", [r"mirpur\s*-?\s*10", r"মিরপুর\s*-?\s*[১০10]+"]),
    ("Dhanmondi 27", [r"dhanmondi\s*-?\s*27", r"ধানমন্ডি\s*-?\s*[২৭27]+"]),
    ("Uttara Sector 7", [r"uttara(?:\s+sector\s*\d+)?", r"উত্তরা(?:\s+সেক্টর\s*[০-৯0-9]+)?"]),
    ("Gulshan 1", [r"gulshan\s*-?\s*[12]", r"গুলশান\s*-?\s*[১২12]+"]),
    ("Motijheel", [r"motijheel", r"মতিঝিল"]),
    ("Shahbagh", [r"shahbagh?", r"শাহবাগ"]),
    ("Banani", [r"banani", r"বনানী"]),
    ("Mohakhali", [r"mohakhali", r"মহাখালী"]),
    ("Chattogram", [r"chattogram|chittagong", r"চট্টগ্রাম"]),
    ("Sylhet", [r"sylhet", r"সিলেট"]),
]

BANGLA_DIGITS = str.maketrans("০১২৩৪৫৬৭৮৯", "0123456789")


def luhn_valid(digits: str) -> bool:
    if len(digits) != 15 or not digits.isdigit():
        return False
    total = 0
    for idx, ch in enumerate(digits):
        d = int(ch)
        if idx % 2 == 1:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


def extract_incident_fields(text: str) -> dict[str, Any]:
    normalized = text.translate(BANGLA_DIGITS).strip()
    lower = normalized.lower()

    bangla_chars = sum(1 for ch in text if "\u0980" <= ch <= "\u09FF")
    latin_chars = sum(1 for ch in text if "a" <= ch.lower() <= "z")
    language = "mixed" if (bangla_chars > 2 and latin_chars > 4) else ("bn" if bangla_chars > 2 else "en")

    # Intent classification
    intent = "general_guidance"
    if any(k in lower for k in ["gd", "general diary", "জিডি", "থানা"]):
        intent = "gd_guidance"
    elif any(k in lower for k in ["sim", "সিম", "block sim", "সিম ব্লক"]):
        intent = "sim_block_help"
    elif any(k in lower for k in ["bkash", "nagad", "rocket", "বিকাশ", "নগদ", "রকেট", "account lock"]):
        intent = "account_protection"
    elif any(k in lower for k in ["stolen", "snatch", "churi", "ছিনতাই", "চুরি", "পকেট"]):
        intent = "report_theft"
    elif any(k in lower for k in ["lost", "missing", "harai", "হারিয়ে", "হারিয়ে", "cannot find"]):
        intent = "report_loss"

    # Device brand
    device_brand = None
    for brand, patterns in BRAND_PATTERNS.items():
        if any(re.search(pat, text, flags=re.IGNORECASE) for pat in patterns):
            device_brand = brand
            break

    # IMEI extraction
    imei_match = re.search(r"\b(\d{15})\b", normalized)
    imei_candidate = imei_match.group(1) if imei_match else None
    imei_luhn_ok = luhn_valid(imei_candidate) if imei_candidate else False

    # Location extraction
    location_mention = None
    for canonical, patterns in DHAKA_AND_BD_LOCATIONS:
        for pat in patterns:
            m = re.search(pat, text, flags=re.IGNORECASE)
            if m:
                location_mention = m.group(0)
                break
        if location_mention:
            break

    # Time / Date extraction
    time_patterns = [
        r"\b\d{1,2}:\d{2}\s*(?:am|pm|টায়|টা|মিনিটে)?",
        r"\b(?:yesterday|today|afternoon|evening|morning|bikale|সন্ধ্যা|দুপুর|সকাল|গতকাল|আজ)\b",
        r"\b\d{1,2}\s+(?:nov|november|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|dec)[a-z]*\s+\d{4}\b",
        r"১৬\s+নভেম্বর\s+২০২৬",
    ]
    time_mentions: list[str] = []
    for pat in time_patterns:
        for m in re.finditer(pat, normalized, flags=re.IGNORECASE):
            val = m.group(0).strip()
            if val not in time_mentions:
                time_mentions.append(val)

    uncertain_fields: list[str] = []
    if not device_brand:
        uncertain_fields.append("device_brand_and_model")
    if not location_mention:
        uncertain_fields.append("incident_location")
    if not time_mentions:
        uncertain_fields.append("incident_time")
    if not imei_candidate:
        uncertain_fields.append("imei_15_digit")

    return {
        "language_detected": language,
        "intent": intent,
        "device_brand": device_brand,
        "location_mention": location_mention,
        "time_mentions": time_mentions,
        "imei_detected": bool(imei_candidate),
        "imei_masked": f"***********{imei_candidate[-4:]}" if imei_candidate else None,
        "imei_luhn_valid": imei_luhn_ok,
        "uncertain_fields": uncertain_fields,
        "requires_user_confirmation": True,
    }
