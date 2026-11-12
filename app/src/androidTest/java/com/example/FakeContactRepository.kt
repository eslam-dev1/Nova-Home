package com.example

import com.example.ui.search.ContactResult

class FakeContactRepository {
    private val contacts = mutableListOf(
        ContactResult("Sarah Connor", "lookup_sarah"),
        ContactResult("John Doe", "lookup_john"),
        ContactResult("Ali Ahmad", "lookup_ali")
    )

    fun getContacts(): List<ContactResult> = contacts.toList()

    fun filterContacts(query: String): List<ContactResult> {
        if (query.isBlank()) return emptyList()
        return contacts.filter { it.name.contains(query, ignoreCase = true) }
    }
}
