package fr.vauguin.listeners_and_reactive_streams.contact

interface ContactListener {
    fun onChange(contacts: Set<Contact>)
}