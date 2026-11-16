package com.example.domain

import android.content.Context
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.IncidentEntity
import com.example.security.KeystoreCryptoManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalKnowledgeDoc(
    val docId: String,
    val title: String,
    val publisher: String,
    val sourceUrl: String,
    val lastVerified: String,
    val content: String,
)

data class LocalCitation(
    val docId: String,
    val title: String,
    val publisher: String,
    val sourceUrl: String,
    val lastVerified: String,
    val excerpt: String,
)

data class LocalRagAnswer(
    val answer: String,
    val grounded: Boolean,
    val citations: List<LocalCitation>,
    val uncertaintyNotice: String?,
)

data class ExplainableAssessment(
    val readinessScore: Int,
    val observedSignals: List<String>,
    val missingInformation: List<String>,
    val ruleTrace: List<Pair<String, Boolean>>,
    val recommendation: String,
    val uncertaintyNotice: String,
)

data class ExtractedNarrativeFields(
    val intent: String,
    val deviceBrand: String?,
    val locationMention: String?,
    val imeiDetected: Boolean,
)

class LocalSecurityEngine(private val context: Context) {

    private val docs: List<LocalKnowledgeDoc> by lazy { loadBundledDocs() }

    private fun loadBundledDocs(): List<LocalKnowledgeDoc> {
        return try {
            val raw = context.assets.open("knowledge_base.json").bufferedReader().use { it.readText() }
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        LocalKnowledgeDoc(
                            docId = o.getString("doc_id"),
                            title = o.getString("title"),
                            publisher = o.getString("publisher"),
                            sourceUrl = o.getString("source_url"),
                            lastVerified = o.getString("last_verified"),
                            content = o.getString("content"),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private val bnExpansions = mapOf(
        "জিডি" to listOf("gd", "general", "diary", "police"),
        "চুরি" to listOf("theft", "stolen", "snatching", "999"),
        "হারিয়ে" to listOf("lost", "missing"),
        "হারানো" to listOf("lost", "missing"),
        "সিম" to listOf("sim", "operator", "121", "grameenphone", "robi"),
        "বিকাশ" to listOf("bkash", "mfs", "16247"),
        "নগদ" to listOf("nagad", "mfs", "16167"),
        "রকেট" to listOf("rocket", "mfs", "16216"),
        "আইএমইআই" to listOf("imei", "btrc", "neir"),
        "৯৯৯" to listOf("999", "emergency"),
    )

    private val stopWords = setOf(
        "a", "an", "and", "are", "at", "can", "do", "for", "how", "i", "in", "is", "it",
        "my", "of", "on", "or", "should", "the", "this", "to", "what", "when", "where",
    )

    fun answerOfflineQuery(query: String, language: String, activeIncident: IncidentEntity?, checklist: List<ChecklistItemEntity>): LocalRagAnswer {
        val qLower = query.lowercase(Locale.ROOT)

        // Check incident-specific prompts first when permitted
        if (activeIncident != null && (qLower.contains("timeline") || qLower.contains("summary") || qLower.contains("status") || qLower.contains("সারাংশ"))) {
            val doneCount = checklist.count { it.isCompleted }
            val ans = if (language == "bn") {
                "সক্রিয় ঘটনা '${activeIncident.title}' (${activeIncident.category})। সম্পন্ন সুরক্ষা ধাপ: $doneCount/${checklist.size}।"
            } else {
                "Active incident '${activeIncident.title}' (${activeIncident.category}, status: ${activeIncident.status}). Completed protection steps: $doneCount/${checklist.size}."
            }
            return LocalRagAnswer(
                answer = ans,
                grounded = true,
                citations = emptyList(),
                uncertaintyNotice = "Generated offline from your confirmed local incident & checklist records.",
            )
        }

        val rawTokens = Regex("[\\u0980-\\u09FFa-z0-9]+").findAll(qLower).map { it.value }.toList()
        val expanded = mutableListOf<String>()
        for (t in rawTokens) {
            if (t !in stopWords && t.length > 1) expanded.add(t)
            bnExpansions.forEach { (k, v) -> if (t.contains(k)) expanded.addAll(v) }
        }

        val scored = docs.map { doc ->
            val hay = "${doc.title} ${doc.content}".lowercase(Locale.ROOT)
            val hits = expanded.distinct().count { tok -> hay.contains(tok) }
            doc to hits
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        if (scored.isEmpty()) {
            val msg = if (language == "bn") {
                "আমাদের অফলাইন যাচাইকৃত জ্ঞানভাণ্ডারে এই প্রশ্নের জন্য পর্যাপ্ত তথ্য নেই। জরুরি প্রয়োজনে ৯৯৯-এ কল করুন অথবা gd.police.gov.bd দেখুন।"
            } else {
                "Insufficient verified evidence in our offline knowledge base to reliably answer this question. For official procedures, check Bangladesh Police Online GD (https://gd.police.gov.bd/) or dial 999."
            }
            return LocalRagAnswer(
                answer = msg,
                grounded = false,
                citations = emptyList(),
                uncertaintyNotice = "No matching verified official document found.",
            )
        }

        val topDocs = scored.take(2).map { it.first }
        val citations = topDocs.map { doc ->
            LocalCitation(
                docId = doc.docId,
                title = doc.title,
                publisher = doc.publisher,
                sourceUrl = doc.sourceUrl,
                lastVerified = doc.lastVerified,
                excerpt = doc.content.take(260),
            )
        }
        val body = topDocs.joinToString("\n\n") { "• ${it.title} (Verified ${it.lastVerified}):\n${it.content.take(360)}..." }
        return LocalRagAnswer(
            answer = body,
            grounded = true,
            citations = citations,
            uncertaintyNotice = "Offline RAG response grounded in bundled official Bangladesh security documentation.",
        )
    }

    fun extractFields(narrative: String): ExtractedNarrativeFields {
        val lower = narrative.lowercase(Locale.ROOT)
        val intent = when {
            lower.contains("gd") || lower.contains("জিডি") -> "gd_guidance"
            lower.contains("sim") || lower.contains("সিম") -> "sim_block_help"
            lower.contains("snatch") || lower.contains("stolen") || lower.contains("ছিনতাই") || lower.contains("চুরি") -> "report_theft"
            else -> "report_loss"
        }
        val brands = listOf("Samsung", "Apple", "iPhone", "Xiaomi", "Redmi", "Realme", "Vivo", "Oppo", "OnePlus", "Tecno", "Symphony", "Walton")
        val brand = brands.firstOrNull { lower.contains(it.lowercase(Locale.ROOT)) }
        val areas = listOf("Farmgate", "Mirpur", "Dhanmondi", "Uttara", "Gulshan", "Motijheel", "Banani", "Shahbagh", "Chattogram", "Sylhet", "ফার্মগেট", "মিরপুর", "ধানমন্ডি", "উত্তরা", "মতিঝিল")
        val loc = areas.firstOrNull { narrative.contains(it, ignoreCase = true) }
        val imeiFound = Regex("\\b\\d{15}\\b").containsMatchIn(narrative)
        return ExtractedNarrativeFields(intent, brand, loc, imeiFound)
    }

    fun assessIncident(
        incident: IncidentEntity,
        checklist: List<ChecklistItemEntity>,
        eventCount: Int,
        bn: Boolean,
    ): ExplainableAssessment {
        val observed = mutableListOf<String>()
        val missing = mutableListOf<String>()
        val trace = mutableListOf<Pair<String, Boolean>>()
        var score = 0

        val hasNarrative = incident.description.trim().length >= 15
        trace.add((if (bn) "ঘটনার বিবরণ (>=১৫ অক্ষর)" else "Narrative length >= 15 chars") to hasNarrative)
        if (hasNarrative) {
            score += 20
            observed.add(if (bn) "ঘটনার বিস্তারিত বিবরণ সংরক্ষিত হয়েছে" else "User narrative recorded")
        } else {
            missing.add(if (bn) "ঘটনার বিস্তারিত বিবরণ লিখুন" else "Detailed incident narrative")
        }

        val hasImei = incident.imeiLast4.isNotBlank()
        trace.add((if (bn) "১৫-ডিজিট IMEI সংযুক্ত" else "15-digit IMEI provided") to hasImei)
        if (hasImei) {
            score += 25
            observed.add(if (bn) "IMEI শেষ ৪ ডিজিট (••••${incident.imeiLast4}) এনক্রিপ্টেড ভল্টে আছে" else "IMEI ending ••••${incident.imeiLast4} encrypted in vault")
        } else {
            missing.add(if (bn) "বক্স/রশিদ থেকে ১৫-ডিজিটের IMEI নম্বর" else "15-digit IMEI from box or invoice")
        }

        val hasDevice = incident.deviceBrand.isNotBlank() && incident.deviceModel.isNotBlank()
        trace.add((if (bn) "ফোনের ব্র্যান্ড ও মডেল" else "Device brand & model") to hasDevice)
        if (hasDevice) {
            score += 15
            observed.add("${incident.deviceBrand} ${incident.deviceModel}")
        } else {
            missing.add(if (bn) "ডিভাইসের ব্র্যান্ড ও মডেল" else "Device brand and model")
        }

        val hasLoc = incident.locationLabel.isNotBlank()
        trace.add((if (bn) "সম্ভাব্য স্থান / এলাকা" else "Location / area recorded") to hasLoc)
        if (hasLoc) {
            score += 15
            observed.add(if (bn) "অবস্থান: ${incident.locationLabel}" else "Location: ${incident.locationLabel}")
        } else {
            missing.add(if (bn) "হারানোর স্থান ও থানা এলাকা" else "Approximate location & Thana area")
        }

        val completedSteps = checklist.count { it.isCompleted }
        val checklistOk = completedSteps >= 2
        trace.add((if (bn) "সুরক্ষা ধাপ (>=২টি সম্পন্ন)" else "Protection checklist (>= 2 done)") to checklistOk)
        if (checklistOk) {
            score += 15
            observed.add(if (bn) "$completedSteps/${checklist.size} সুরক্ষা ধাপ সম্পন্ন" else "$completedSteps/${checklist.size} protection steps completed")
        } else {
            missing.add(if (bn) "অবিলম্বে সিম ও বিকাশ/নগদ ব্লক ধাপ সম্পন্ন করুন" else "Complete SIM & MFS blocking steps")
        }

        val hasEvents = eventCount >= 1
        trace.add((if (bn) "টাইমলাইন ইভেন্ট সংরক্ষিত" else "Timeline events logged") to hasEvents)
        if (hasEvents) {
            score += 10
            observed.add(if (bn) "$eventCount টি টাইমলাইন রেকর্ড" else "$eventCount timeline event(s) logged")
        } else {
            missing.add(if (bn) "প্রাথমিক টাইমলাইন নোট" else "Initial timeline observation")
        }

        val rec = if (incident.category == "snatching") {
            if (bn) "ছিনতাইয়ের ক্ষেত্রে অবিলম্বে ৯৯৯-এ কল করুন বা নিকটস্থ থানায় যান; এরপর সিম ও MFS বন্ধ করুন।"
            else "For snatching, contact 999 or nearest Thana immediately, then block your SIM and MFS accounts."
        } else {
            if (bn) "প্রথমে সিম ও বিকাশ/নগদ লক করুন, Find Hub-এ ফোন লক করুন, তারপর Online GD খসড়া তৈরি করুন।"
            else "Block SIM & MFS wallets first, lock via Find Hub, then review your Online GD draft."
        }

        val uncertainty = if (bn) {
            "এই স্কোর শুধুমাত্র তথ্য সম্পূর্ণতা নির্দেশ করে — কোনো অ্যালগরিদম ফোন চুরি নিশ্চিত করে না।"
        } else {
            "Readiness score reflects documentation completeness only — AI never declares a phone stolen."
        }
        return ExplainableAssessment(score, observed, missing, trace, rec, uncertainty)
    }

    fun buildReportDraft(
        incident: IncidentEntity,
        checklist: List<ChecklistItemEntity>,
        bn: Boolean,
    ): String {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val occurredStr = dateFmt.format(Date(incident.occurredAtMillis))
        val doneList = checklist.filter { it.isCompleted }.map { if (bn) it.titleBn else it.titleEn }
        val assessment = assessIncident(incident, checklist, 1, bn)
        val imeiStr = KeystoreCryptoManager.maskImei(KeystoreCryptoManager.decrypt(incident.imeiEncrypted))

        return buildString {
            appendLine(
                if (bn) "[শুধুমাত্র খসড়া — এটি বাংলাদেশ পুলিশে দাখিলকৃত অফিসিয়াল জিডি নয়]"
                else "[DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED GENERAL DIARY]"
            )
            appendLine("========================================")
            appendLine("Incident ID: ${incident.id.take(8).uppercase()}")
            appendLine("Category: ${BilingualStrings.categoryLabel(incident.category, bn)}")
            appendLine("Date & Time: $occurredStr")
            appendLine("Location: ${incident.locationLabel.ifBlank { "Not provided" }}")
            appendLine("Device: ${incident.deviceBrand} ${incident.deviceModel}".trim().ifBlank { "Not provided" })
            appendLine("IMEI (Masked): $imeiStr")
            appendLine()
            appendLine(if (bn) "ঘটনার বিবরণ (ব্যবহারকারী নিশ্চিত):" else "User-Confirmed Incident Narrative:")
            appendLine(incident.description.ifBlank { "No narrative provided yet." })
            appendLine()
            appendLine(if (bn) "সম্পন্নকৃত সুরক্ষা পদক্ষেপ:" else "Protection Actions Completed:")
            if (doneList.isEmpty()) {
                appendLine("- None recorded yet")
            } else {
                doneList.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine(if (bn) "অপূর্ণ তথ্য (GD করার আগে যাচাই করুন):" else "Missing Information to Complete Before Filing Online GD:")
            if (assessment.missingInformation.isEmpty()) {
                appendLine("- Core fields present. Bring NID & original purchase invoice.")
            } else {
                assessment.missingInformation.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Official Bangladesh Police Online GD Portal: https://gd.police.gov.bd/")
        }
    }
}
