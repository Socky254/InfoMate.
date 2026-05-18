package com.infomate.app.core

import android.content.Context
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import java.util.*

class NeuralIngestor(private val context: Context) {

    fun captureUserPatterns(): String {
        val summary = StringBuilder("### USER DATA PATTERNS ###\n")
        
        try {
            summary.append(getContactInsights())
            summary.append(getCalendarEvents())
            summary.append(getCallLogInsights())
            summary.append(getSmsInsights())
        } catch (e: Exception) {
            Log.e("NeuralIngestor", "Error capturing patterns: ${e.message}")
            summary.append("Data Ingestion Incomplete: ${e.message}\n")
        }
        
        return summary.toString()
    }

    private fun getContactInsights(): String {
        val contacts = mutableListOf<String>()
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection, null, null, null
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (nameIndex == -1) return "Top Contacts: Unavailable\n"
            
            var count = 0
            while (it.moveToNext() && count < 10) {
                val name = it.getString(nameIndex)
                if (!name.isNullOrBlank()) {
                    contacts.add(name)
                    count++
                }
            }
        }
        return "Top Contacts: ${contacts.joinToString(", ")}\n"
    }

    private fun getCalendarEvents(): String {
        val events = mutableListOf<String>()
        val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            "${CalendarContract.Events.DTSTART} > ?",
            arrayOf(System.currentTimeMillis().toString()),
            "${CalendarContract.Events.DTSTART} ASC"
        )
        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            if (titleIndex == -1) return "Upcoming Events: Unavailable\n"

            var count = 0
            while (it.moveToNext() && count < 5) {
                val title = it.getString(titleIndex)
                if (!title.isNullOrBlank()) {
                    events.add(title)
                    count++
                }
            }
        }
        return "Upcoming Events: ${events.joinToString(", ")}\n"
    }

    private fun getCallLogInsights(): String {
        val logs = mutableListOf<String>()
        val projection = arrayOf(CallLog.Calls.NUMBER)
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null, null, "${CallLog.Calls.DATE} DESC"
        )
        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            if (numberIndex == -1) return "Recent Interactions: Unavailable\n"

            var count = 0
            while (it.moveToNext() && count < 5) {
                val number = it.getString(numberIndex)
                if (!number.isNullOrBlank()) {
                    logs.add(number)
                    count++
                }
            }
        }
        return "Recent Interactions (Numbers): ${logs.distinct().joinToString(", ")}\n"
    }

    private fun getSmsInsights(): String {
        val snippets = mutableListOf<String>()
        val projection = arrayOf(Telephony.Sms.BODY)
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            null, null, "${Telephony.Sms.DATE} DESC"
        )
        cursor?.use {
            val bodyIndex = it.getColumnIndex(Telephony.Sms.BODY)
            if (bodyIndex == -1) return "Recent SMS Context: Unavailable\n"

            var count = 0
            while (it.moveToNext() && count < 3) {
                val body = it.getString(bodyIndex)
                if (!body.isNullOrBlank()) {
                    if (body.length > 50) snippets.add(body.take(50) + "...") else snippets.add(body)
                    count++
                }
            }
        }
        return "Recent SMS Context: ${snippets.joinToString(" | ")}\n"
    }
}
