package com.example.ui.search

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactResult(val name: String, val lookupKey: String)

object ContactSearchHelper {
    fun searchContacts(context: Context, query: String): List<ContactResult> {
        val results = mutableListOf<ContactResult>()
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return results
        }
        if (query.isBlank()) return results

        val resolver: ContentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val keyIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                while (cursor.moveToNext()) {
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "Unknown"
                    val key = if (keyIndex >= 0) cursor.getString(keyIndex) else ""
                    results.add(ContactResult(name, key))
                }
            }
        } catch (e: Exception) {
            // Graceful degradation on query failure
        }
        return results.distinctBy { it.name }
    }
}
