package com.infomate.app.core

import android.content.Context
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Telephony
import java.util.*

class NeuralIngestor(private val context: Context) {

    fun captureUserPatterns(): String {
        val summary = StringBuilder("### USER DATA PATTERNS ###\n")
        
        summary.append(getContactInsights())
        summary.append(getCalendarEvents())
        summary.append(getCallLogInsights())
        summary.append(getSmsInsights())
        
        return summary.toString()
    }

    private fun getContactInsights(): String {
        val contacts = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 10) { // Get top 10 for context
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME) ?: 0)
                contacts.add(name)
                count++
            }
        }
        return "Top Contacts: ${contacts.joinToString(", ")}\n"
    }

    private fun getCalendarEvents(): String {
        val events = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
            "${CalendarContract.Events.DTSTART} > ?",
            arrayOf(System.currentTimeMillis().toString()),
            "${CalendarContract.Events.DTSTART} ASC"
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 5) {
                val title = it.getString(0)
                events.add(title)
                count++
            }
        }
        return "Upcoming Events: ${events.joinToString(", ")}\n"
    }

    private fun getCallLogInsights(): String {
        val logs = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC"
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 5) {
                val number = it.getString(0)
                logs.add(number)
                count++
            }
        }
        return "Recent Interactions (Numbers): ${logs.distinct().joinToString(", ")}\n"
    }

    private fun getSmsInsights(): String {
        val snippets = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.BODY),
            null, null, "${Telephony.Sms.DATE} DESC"
        )
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < 3) {
                val body = it.getString(0)
                if (body.length > 50) snippets.add(body.take(50) + "...") else snippets.add(body)
                count++
            }
        }
        return "Recent SMS Context: ${snippets.joinToString(" | ")}\n"
    }
}
