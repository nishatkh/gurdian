package com.example.domain

object BilingualStrings {
    fun navLabel(tab: String, bn: Boolean): String = when (tab) {
        "home" -> if (bn) "হোম" else "Home"
        "incidents" -> if (bn) "ঘটনা" else "Incidents"
        "emergency" -> if (bn) "জরুরি" else "Emergency"
        "activity" -> if (bn) "কার্যক্রম" else "Activity"
        "profile" -> if (bn) "প্রোফাইল" else "Profile"
        else -> tab
    }

    fun greeting(name: String, bn: Boolean): String =
        if (bn) "স্বাগতম, $name" else "Welcome back, $name"

    fun subtitleHome(bn: Boolean): String =
        if (bn) "বাংলাদেশ মোবাইল নিরাপত্তা ও তাৎক্ষণিক সহায়তা"
        else "Mobile theft response & account protection"

    fun emergencyTitle(bn: Boolean): String =
        if (bn) "জরুরি চুরি / হারানো মোড" else "Emergency / Theft Mode"

    fun emergencyDesc(bn: Boolean): String =
        if (bn) "আপনার ফোন হারানো বা চুরি হলে ধাপে ধাপে সুরক্ষা প্রক্রিয়া শুরু করুন।"
        else "Start guided incident logging, SIM/MFS lock steps & GD preparation."

    fun disclaimer(bn: Boolean): String =
        if (bn) "Guardian AI একটি সহায়ক অ্যাপ। এটি বাংলাদেশ পুলিশ, Online GD, ৯৯৯, BTRC/NEIR বা ব্যাংক/MFS-এর বিকল্প নয়।"
        else "Guardian AI is a response companion. It does not replace Bangladesh Police, Online GD, 999, BTRC/NEIR, or your bank/MFS provider."

    fun statusLabel(status: String, bn: Boolean): String = when (status) {
        "draft" -> if (bn) "খসড়া" else "Draft"
        "active" -> if (bn) "সক্রিয়" else "Active"
        "submitted_for_review" -> if (bn) "পর্যালোচনার জন্য জমা" else "Submitted for review"
        "under_review" -> if (bn) "পর্যালোচনাধীন" else "Under review"
        "additional_info_requested" -> if (bn) "অতিরিক্ত তথ্য প্রয়োজন" else "Additional info requested"
        "approved" -> if (bn) "অনুমোদিত" else "Approved"
        "closed" -> if (bn) "সম্পন্ন" else "Closed"
        "cancelled" -> if (bn) "বাতিল" else "Cancelled"
        else -> status
    }

    fun categoryLabel(cat: String, bn: Boolean): String = when (cat) {
        "lost_phone" -> if (bn) "ফোন হারানো" else "Lost Phone"
        "suspected_theft" -> if (bn) "চুরি সন্দেহ" else "Suspected Theft"
        "snatching" -> if (bn) "ছিনতাই" else "Snatching"
        "sim_compromise" -> if (bn) "সিম ঝুঁকি" else "SIM Compromise"
        "account_compromise" -> if (bn) "অ্যাকাউন্ট ঝুঁকি" else "Account Compromise"
        else -> cat
    }
}
