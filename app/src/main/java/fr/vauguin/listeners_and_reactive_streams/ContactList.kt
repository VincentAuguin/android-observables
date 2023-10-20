package fr.vauguin.listeners_and_reactive_streams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.vauguin.listeners_and_reactive_streams.contact.ContactManagerImpl
import kotlinx.coroutines.Dispatchers

data class ContactListItemUiState(
    val id: String,
    val name: String,
    val phoneNumber: String,
)

@Composable
fun ContactList(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel = viewModel(initializer = {
        ContactListViewModel(
            contactManager = ContactManagerImpl(
                androidContext = context.applicationContext,
                coroutineContext = Dispatchers.Default
            )
        )
    })

    val contacts by viewModel.uiState.collectAsState()

    LazyColumn(modifier = modifier) {
        items(contacts, key = { item -> item.id }) { item ->
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(item.name)
                Text(item.phoneNumber)
            }
        }
    }
}