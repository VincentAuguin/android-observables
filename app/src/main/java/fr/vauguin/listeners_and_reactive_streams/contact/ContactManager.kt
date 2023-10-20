package fr.vauguin.listeners_and_reactive_streams.contact

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

interface ContactManager {
    val contacts: Flow<Set<Contact>>

    fun addListener(listener: ContactListener)

    fun removeListener(listener: ContactListener)
}

class ContactManagerImpl(
    private val androidContext: Context,
    coroutineContext: CoroutineContext,
) : ContactManager {

    private val scope = CoroutineScope(coroutineContext)

    private val listeners = mutableSetOf<ContactListener>()

    private var listenersJob: Job? = null

    private val contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                emitAllContacts()
            }
        }

    private val _contacts = MutableSharedFlow<Set<Contact>>(replay = 1)

    override val contacts: Flow<Set<Contact>> = _contacts.asSharedFlow()

    init {
        emitAllContacts()
        observeContactUpdates()

        scope.launch {
            contacts.collect { contacts ->
                listeners.forEach { it.onChange(contacts) }
            }
        }
    }

    override fun addListener(listener: ContactListener) {
        listeners.add(listener)

        if (listenersJob == null) {
            listenersJob = scope.launch {
                contacts.collect { contacts -> listeners.forEach { it.onChange(contacts) } }
            }
        }
    }

    override fun removeListener(listener: ContactListener) {
        listeners.remove(listener)


        if (listeners.isEmpty()) {
            listenersJob?.cancel()
            listenersJob = null
        }
    }

    private fun observeContactUpdates(): Boolean {
        if (!canRequestContacts()) {
            Log.w(
                TAG,
                "Unable to register a ContentObserver for contacts because permission is not granted"
            )
            return false
        }

        androidContext.contentResolver.unregisterContentObserver(contentObserver)
        androidContext.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            contentObserver
        )
        return true
    }

    private fun emitAllContacts() {
        if (canRequestContacts()) {
            scope.launch {
                _contacts.emit(getAllContacts())
            }
        }
    }

    private fun getAllContacts(): Set<Contact> {
        val cursor = androidContext.contentResolver.cursorForContacts()
        return cursor?.use {
            buildSet {
                while (cursor.moveToNext()) {
                    cursor.toContact().let { contact ->
                        if (!any { it.id == contact.id }) {
                            add(contact)
                        }
                    }
                }
            }
        } ?: emptySet()
    }

    private fun canRequestContacts(): Boolean {
        return androidContext.checkCallingOrSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    private fun Cursor.toContact(): Contact {
        return Contact(
            id = getLong(0).toString(),
            name = getString(1),
            phoneNumber = getString(2),
        )
    }

    private fun ContentResolver.cursorForContacts(): Cursor? {
        if (!canRequestContacts()) {
            return null
        }

        return query(
            ContactsContract.Data.CONTENT_URI,
            CONTACT_PROJECTION,
            ALL_CONTACTS_SELECTION,
            null,
            null
        )
    }

    companion object {
        private const val TAG = "Contact"
        private const val ALL_CONTACTS_SELECTION =
            "${ContactsContract.Data.MIMETYPE}='${ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE}'"
        private val CONTACT_PROJECTION =
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.Data.DATA1
            )
    }
}