package fr.vauguin.listeners_and_reactive_streams

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.vauguin.listeners_and_reactive_streams.contact.ContactManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ContactListViewModel(contactManager: ContactManager) : ViewModel() {

    val uiState: StateFlow<ImmutableList<ContactListItemUiState>> = contactManager.contacts
        .map {
            it.map { contact ->
                ContactListItemUiState(
                    id = contact.id,
                    name = contact.name,
                    phoneNumber = contact.phoneNumber
                )
            }.toImmutableList()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = persistentListOf()
        )
}
