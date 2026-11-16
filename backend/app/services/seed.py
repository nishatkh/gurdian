from __future__ import annotations

from datetime import datetime, timezone
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.entities import (
    ChecklistItem,
    EmergencyResource,
    ProtectionChecklist,
    User,
    UserProfile,
)

DEFAULT_PROTECTION_STEPS = [
    (
        "sim_operator",
        1,
        "Block Active SIM Card Immediately",
        "Call your mobile operator customer care (GP/Robi/Banglalink/Teletalk: 121 from same network; GP from other: 01711594594) with NID verification to temporarily block the lost SIM.",
        "Prevents attackers from intercepting SMS OTPs for banking, MFS, and email recovery.",
        "https://www.grameenphone.com/personal/customer-service",
    ),
    (
        "mfs",
        2,
        "Suspend MFS Wallets (bKash / Nagad / Rocket)",
        "Contact official MFS helplines (bKash: 16247, Nagad: 16167, Rocket: 16216) to temporarily freeze mobile wallet access.",
        "Protects wallet balances and prevents unauthorized SIM-based PIN resets.",
        "https://www.bkash.com/en/help",
    ),
    (
        "banking",
        3,
        "Alert Banking Apps & Freeze Cards if Linked",
        "Call your bank's official 24/7 hotline to disable mobile banking sessions and SMS alert bindings on the lost device.",
        "Stops unauthorized fund transfers from logged-in mobile banking apps.",
        None,
    ),
    (
        "google_account",
        4,
        "Secure Device via Android Find Hub & Change Google Password",
        "Open https://www.google.com/android/find/ to lock the device, then sign out the lost device and change your Google password at https://myaccount.google.com/security.",
        "Protects Gmail, Google Drive, saved passwords, and enables remote screen locking.",
        "https://www.google.com/android/find/",
    ),
    (
        "apple_account",
        5,
        "Enable Apple Find My Lost Mode (If iPhone/iPad)",
        "Open https://www.icloud.com/find to activate Lost Mode and update your Apple Account password.",
        "Locks the iPhone with Activation Lock and suspends Apple Pay.",
        "https://support.apple.com/en-us/101593",
    ),
    (
        "email",
        6,
        "Revoke Active Sessions on Primary Email Accounts",
        "Sign out of all other sessions on Gmail, Outlook, or Yahoo and review 2-step verification recovery numbers.",
        "Email is the reset key for almost every other digital service.",
        "https://myaccount.google.com/device-activity",
    ),
    (
        "social_media",
        7,
        "Log Out Social Media Sessions (Facebook, Instagram, X)",
        "Open Security & Login settings from another trusted device and choose 'Log out of all sessions'.",
        "Prevents impersonation or scam messages sent to your friends and family.",
        "https://www.facebook.com/help/211990645501187",
    ),
    (
        "messaging",
        8,
        "Re-register or Secure WhatsApp, Telegram & Messenger",
        "Enable Two-Step Verification PIN and re-claim WhatsApp/Telegram once your replacement SIM is active.",
        "Stops attackers from accessing chat threads or defrauding contacts.",
        "https://faq.whatsapp.com/1131652977717250",
    ),
    (
        "police_gd",
        9,
        "File Official Police General Diary (Online GD / Thana)",
        "Use your verified Guardian AI draft as reference to submit an official GD at https://gd.police.gov.bd/ or visit the nearest Thana with NID and IMEI invoice.",
        "Creates the legal record required for IMEI blocking/tracking and SIM replacement.",
        "https://gd.police.gov.bd/",
    ),
]

VERIFIED_RESOURCES = [
    {
        "id": "res_bd_999",
        "name": "National Emergency Service 999 (Bangladesh)",
        "category": "emergency_service",
        "address": "Nationwide Toll-Free Emergency Dispatch, Bangladesh Police",
        "contact_number": "999",
        "source_url": "https://nhd.gov.bd/999",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_bd_online_gd",
        "name": "Bangladesh Police Online GD Portal",
        "category": "police_portal",
        "address": "Online General Diary Service, Bangladesh Police",
        "contact_number": "gd.police.gov.bd",
        "source_url": "https://gd.police.gov.bd/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_btrc_neir",
        "name": "BTRC & NEIR Handset Registry",
        "category": "government_regulator",
        "address": "IEB Bhaban, Ramna, Dhaka 1000",
        "contact_number": "100 (BTRC Shortcode)",
        "source_url": "https://www.btrc.gov.bd/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_find_hub",
        "name": "Google Android Find Hub",
        "category": "official_support",
        "address": "Official Google Device Protection Portal",
        "contact_number": "Web Portal",
        "source_url": "https://www.google.com/android/find/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_apple_find_my",
        "name": "Apple Find My (iCloud)",
        "category": "official_support",
        "address": "Official Apple Device Protection Portal",
        "contact_number": "Web Portal",
        "source_url": "https://www.icloud.com/find",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_gp_care",
        "name": "Grameenphone Customer Care",
        "category": "mobile_operator",
        "address": "Nationwide Operator Support (Bangladesh)",
        "contact_number": "121 / 01711594594",
        "source_url": "https://www.grameenphone.com/personal/customer-service",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_robi_care",
        "name": "Robi & Airtel Customer Care",
        "category": "mobile_operator",
        "address": "Nationwide Operator Support (Bangladesh)",
        "contact_number": "121",
        "source_url": "https://www.robi.com.bd/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_bl_care",
        "name": "Banglalink Customer Care",
        "category": "mobile_operator",
        "address": "Nationwide Operator Support (Bangladesh)",
        "contact_number": "121",
        "source_url": "https://www.banglalink.net/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_bkash",
        "name": "bKash Official Customer Care",
        "category": "mfs_provider",
        "address": "Bangladesh MFS Support",
        "contact_number": "16247",
        "source_url": "https://www.bkash.com/en/help",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_nagad",
        "name": "Nagad Official Customer Care",
        "category": "mfs_provider",
        "address": "Bangladesh MFS Support",
        "contact_number": "16167 / 09609616167",
        "source_url": "https://nagad.com.bd/",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
    {
        "id": "res_rocket",
        "name": "DBBL Rocket Customer Care",
        "category": "mfs_provider",
        "address": "Dutch-Bangla Bank MFS Support",
        "contact_number": "16216",
        "source_url": "https://www.dutchbanglabank.com/rocket/rocket.html",
        "verification_date": "2026-11-16",
        "is_verified": True,
    },
]


def ensure_default_checklist_for_user(db: Session, user_id: str) -> ProtectionChecklist:
    existing = (
        db.query(ProtectionChecklist)
        .filter(ProtectionChecklist.user_id == user_id, ProtectionChecklist.incident_id.is_(None))
        .first()
    )
    if existing:
        return existing
    checklist = ProtectionChecklist(user_id=user_id, title="Account & Device Protection Checklist")
    db.add(checklist)
    db.flush()
    for cat, order, title, what, why, url in DEFAULT_PROTECTION_STEPS:
        db.add(
            ChecklistItem(
                checklist_id=checklist.id,
                category=cat,
                sort_order=order,
                title=title,
                what_to_do=what,
                why_it_matters=why,
                official_url=url,
            )
        )
    return checklist


def seed_initial_data(db: Session) -> None:
    # Seed verified emergency resources
    for item in VERIFIED_RESOURCES:
        if not db.get(EmergencyResource, item["id"]):
            db.add(EmergencyResource(**item))

    # Seed default admin/reviewer accounts only if no admin exists yet (development convenience)
    admin_email = "admin@guardianai.app"
    if not db.query(User).filter(User.email == admin_email).first():
        admin = User(
            email=admin_email,
            phone="+8801700000000",
            password_hash=hash_password("GuardianAdmin!2026"),
            role="admin",
        )
        db.add(admin)
        db.flush()
        db.add(
            UserProfile(
                user_id=admin.id,
                full_name="Guardian Security Reviewer",
                preferred_language="en",
                district="Dhaka",
            )
        )
        ensure_default_checklist_for_user(db, admin.id)

    db.commit()
