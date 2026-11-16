package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IncidentEntity::class,
        IncidentEventEntity::class,
        IncidentEvidenceEntity::class,
        ChecklistItemEntity::class,
        TrustedContactEntity::class,
        ContactShareLogEntity::class,
        EmergencyResourceEntity::class,
        SyncOutboxEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun trustedContactDao(): TrustedContactDao
    abstract fun resourceDao(): ResourceDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: GuardianDatabase? = null

        fun getInstance(context: Context): GuardianDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    "guardian_ai_offline.db",
                ).build().also { INSTANCE = it }
            }
        }

        fun defaultChecklistSeed(): List<ChecklistItemEntity> = listOf(
            ChecklistItemEntity(
                id = "chk_sim",
                category = "sim_operator",
                sortOrder = 1,
                titleEn = "Block Lost SIM via Mobile Operator",
                titleBn = "মোবাইল অপারেটরের মাধ্যমে সিম ব্লক করুন",
                whatToDoEn = "Call GP/Robi/Banglalink/Teletalk customer care (121 from same network; GP from other: 01711594594) with your NID details to suspend the SIM immediately.",
                whatToDoBn = "আপনার NID তথ্য দিয়ে গ্রামীণফোন/রবি/বাংলালিংক/টেলিটক কাস্টমার কেয়ারে (স্ব-স্ব নেটওয়ার্ক থেকে ১২১; অন্য নম্বর থেকে জিপি: ০১৭১১৫৯৪৫৯৪) কল করে দ্রুত সিম স্থগিত করুন।",
                whyItMattersEn = "Stops attackers from intercepting SMS OTPs for banking, MFS, and email resets.",
                whyItMattersBn = "ব্যাংক, বিকাশ/নগদ এবং ইমেইল অ্যাকাউন্টের OTP চুরি হওয়া আটকায়।",
                officialUrl = "https://www.grameenphone.com/personal/customer-service",
                officialPhone = "121",
            ),
            ChecklistItemEntity(
                id = "chk_mfs",
                category = "mfs",
                sortOrder = 2,
                titleEn = "Freeze MFS Wallets (bKash / Nagad / Rocket)",
                titleBn = "বিকাশ / নগদ / রকেট অ্যাকাউন্ট ফ্রিজ করুন",
                whatToDoEn = "Call official MFS helplines (bKash: 16247, Nagad: 16167, Rocket: 16216) to temporarily lock your mobile wallet. Never share your PIN or OTP.",
                whatToDoBn = "বিকাশ (১৬২৪৭), নগদ (১৬১৬৭) বা রকেট (১৬২১৬) অফিসিয়াল হেল্পলাইনে কল করে অ্যাকাউন্ট সাময়িকভাবে লক করুন। কখনো পিন বা ওটিপি শেয়ার করবেন না।",
                whyItMattersEn = "Protects mobile wallet funds before SIM-based takeover attempts.",
                whyItMattersBn = "মোবাইল ওয়ালেটের টাকা অননুমোদিত লেনদেন থেকে সুরক্ষিত রাখে।",
                officialUrl = "https://www.bkash.com/en/help",
                officialPhone = "16247",
            ),
            ChecklistItemEntity(
                id = "chk_bank",
                category = "banking",
                sortOrder = 3,
                titleEn = "Alert Banking Apps & Freeze Cards",
                titleBn = "ব্যাংকিং অ্যাপ ও কার্ড সুরক্ষিত করুন",
                whatToDoEn = "Call your bank's 24/7 hotline from another phone to deactivate mobile banking sessions and SMS OTP alerts linked to the lost phone.",
                whatToDoBn = "অন্য ফোন থেকে আপনার ব্যাংকের ২৪/৭ হটলাইনে কল করে মোবাইল ব্যাংকিং সেশন বন্ধ করুন।",
                whyItMattersEn = "Prevents unauthorized transfers through logged-in banking apps.",
                whyItMattersBn = "লগইন থাকা ব্যাংকিং অ্যাপ দিয়ে অননুমোদিত লেনদেন প্রতিরোধ করে।",
                officialUrl = null,
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_google",
                category = "google_account",
                sortOrder = 4,
                titleEn = "Lock Device via Android Find Hub & Rotate Google Password",
                titleBn = "Android Find Hub দিয়ে লক ও গুগল পাসওয়ার্ড পরিবর্তন",
                whatToDoEn = "Open https://www.google.com/android/find/ from a trusted device to secure/lock the phone, then change your Google password. Avoid erasing before saving needed records.",
                whatToDoBn = "অন্য ডিভাইস থেকে https://www.google.com/android/find/ খুলে ফোনটি লক করুন এবং গুগল পাসওয়ার্ড পরিবর্তন করুন।",
                whyItMattersEn = "Protects Gmail, Google Photos, saved passwords, and enables remote lock.",
                whyItMattersBn = "জিমেইল, ড্রাইভ ও সেভ করা পাসওয়ার্ড সুরক্ষিত রাখে।",
                officialUrl = "https://www.google.com/android/find/",
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_apple",
                category = "apple_account",
                sortOrder = 5,
                titleEn = "Enable Apple Find My Lost Mode (If iPhone)",
                titleBn = "Apple Find My Lost Mode চালু করুন (iPhone হলে)",
                whatToDoEn = "Sign in at https://www.icloud.com/find to turn on Lost Mode and update your Apple Account password.",
                whatToDoBn = "https://www.icloud.com/find এ লগইন করে Lost Mode চালু করুন এবং Apple অ্যাকাউন্টের পাসওয়ার্ড বদলান।",
                whyItMattersEn = "Activates Activation Lock and suspends Apple Pay cards.",
                whyItMattersBn = "অ্যাক্টিভেশন লক সক্রিয় করে এবং অননুমোদিত ব্যবহার রোধ করে।",
                officialUrl = "https://support.apple.com/en-us/101593",
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_email",
                category = "email",
                sortOrder = 6,
                titleEn = "Revoke Active Email Sessions",
                titleBn = "প্রাথমিক ইমেইলের সেশন লগ-আউট করুন",
                whatToDoEn = "Sign out all sessions on Gmail/Outlook/Yahoo and remove the lost SIM from instant recovery if needed.",
                whatToDoBn = "আপনার ইমেইলের সব ডিভাইস থেকে সেশন সাইন-আউট করুন।",
                whyItMattersEn = "Primary email controls password resets for most online accounts.",
                whyItMattersBn = "ইমেইল সুরক্ষিত থাকলে অন্য অ্যাকাউন্টের পাসওয়ার্ড রিসেট করা যায় না।",
                officialUrl = "https://myaccount.google.com/device-activity",
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_social",
                category = "social_media",
                sortOrder = 7,
                titleEn = "Sign Out Social Media (Facebook / Instagram)",
                titleBn = "সোশ্যাল মিডিয়া সেশন লগ-আউট করুন",
                whatToDoEn = "Open Security & Login on Facebook/Instagram from another device and log out of the missing phone session.",
                whatToDoBn = "অন্য ডিভাইস থেকে Facebook/Instagram সেটিংসে গিয়ে হারানো ফোনের সেশন লগ-আউট করুন।",
                whyItMattersEn = "Prevents impersonation scams sent to your contacts.",
                whyItMattersBn = "আপনার পরিচিতদের কাছে প্রতারণামূলক মেসেজ যাওয়া আটকায়।",
                officialUrl = "https://www.facebook.com/help/211990645501187",
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_msg",
                category = "messaging",
                sortOrder = 8,
                titleEn = "Secure WhatsApp & Telegram",
                titleBn = "হোয়াটসঅ্যাপ ও টেলিগ্রাম সুরক্ষিত করুন",
                whatToDoEn = "Once your replacement SIM is active, re-register WhatsApp/Telegram and enable Two-Step Verification PIN.",
                whatToDoBn = "নতুন সিম তোলার পর WhatsApp/Telegram রি-রেজিস্টার করে Two-Step Verification চালু করুন।",
                whyItMattersEn = "Reclaims your messaging identity and protects group chats.",
                whyItMattersBn = "মেসেজিং অ্যাকাউন্টের নিয়ন্ত্রণ ফিরিয়ে আনে।",
                officialUrl = "https://faq.whatsapp.com/1131652977717250",
                officialPhone = null,
            ),
            ChecklistItemEntity(
                id = "chk_gd",
                category = "police_gd",
                sortOrder = 9,
                titleEn = "File Official Bangladesh Police Online GD / Thana GD",
                titleBn = "বাংলাদেশ পুলিশ অনলাইন জিডি বা থানায় জিডি করুন",
                whatToDoEn = "Prepare your Guardian AI draft, gather NID + purchase invoice + 15-digit IMEI, and file at https://gd.police.gov.bd/ or your local Thana.",
                whatToDoBn = "Guardian AI খসড়া, জাতীয় পরিচয়পত্র, ক্যাশ মেমো ও IMEI নিয়ে https://gd.police.gov.bd/ অথবা নিকটস্থ থানায় জিডি করুন।",
                whyItMattersEn = "Official GD is mandatory for BTRC/NEIR tracking and SIM replacement.",
                whyItMattersBn = "IMEI ট্র্যাকিং এবং আইনি সুরক্ষার জন্য অফিসিয়াল জিডি অপরিহার্য।",
                officialUrl = "https://gd.police.gov.bd/",
                officialPhone = "999",
            ),
        )

        fun defaultResourcesSeed(): List<EmergencyResourceEntity> = listOf(
            EmergencyResourceEntity(
                id = "res_bd_999",
                name = "National Emergency Service 999 (Bangladesh Police)",
                category = "Emergency Services",
                address = "Nationwide 24/7 Toll-Free Dispatch",
                contactNumber = "999",
                sourceUrl = "https://nhd.gov.bd/999",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_online_gd",
                name = "Bangladesh Police Online GD Portal",
                category = "Police & Government",
                address = "Official Online General Diary Portal",
                contactNumber = "Web Portal",
                sourceUrl = "https://gd.police.gov.bd/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_btrc_neir",
                name = "BTRC & NEIR Handset Registry",
                category = "Police & Government",
                address = "IEB Bhaban, Ramna, Dhaka 1000",
                contactNumber = "100",
                sourceUrl = "https://www.btrc.gov.bd/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_find_hub",
                name = "Google Android Find Hub",
                category = "Official Device Support",
                address = "Official Android Remote Lock & Locate",
                contactNumber = "Web Portal",
                sourceUrl = "https://www.google.com/android/find/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_apple_find",
                name = "Apple Find My (iCloud)",
                category = "Official Device Support",
                address = "Official Apple Lost Mode Portal",
                contactNumber = "Web Portal",
                sourceUrl = "https://www.icloud.com/find",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_gp",
                name = "Grameenphone Customer Care",
                category = "Mobile Operators",
                address = "Nationwide Operator Support",
                contactNumber = "121 / 01711594594",
                sourceUrl = "https://www.grameenphone.com/personal/customer-service",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_robi",
                name = "Robi / Airtel Customer Care",
                category = "Mobile Operators",
                address = "Nationwide Operator Support",
                contactNumber = "121",
                sourceUrl = "https://www.robi.com.bd/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_bl",
                name = "Banglalink Customer Care",
                category = "Mobile Operators",
                address = "Nationwide Operator Support",
                contactNumber = "121",
                sourceUrl = "https://www.banglalink.net/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_bkash",
                name = "bKash Official Customer Care",
                category = "Mobile Financial Services",
                address = "24/7 Account Lock Support",
                contactNumber = "16247",
                sourceUrl = "https://www.bkash.com/en/help",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_nagad",
                name = "Nagad Official Customer Care",
                category = "Mobile Financial Services",
                address = "24/7 Account Lock Support",
                contactNumber = "16167",
                sourceUrl = "https://nagad.com.bd/",
                verificationDate = "2026-11-16",
            ),
            EmergencyResourceEntity(
                id = "res_rocket",
                name = "DBBL Rocket Customer Care",
                category = "Mobile Financial Services",
                address = "Dutch-Bangla Bank Support",
                contactNumber = "16216",
                sourceUrl = "https://www.dutchbanglabank.com/rocket/rocket.html",
                verificationDate = "2026-11-16",
            ),
        )
    }
}
