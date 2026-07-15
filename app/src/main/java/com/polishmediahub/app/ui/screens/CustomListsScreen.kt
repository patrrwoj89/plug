package com.polishmediahub.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.polishmediahub.app.R
import com.polishmediahub.app.ui.components.FocusableSurface
import com.polishmediahub.app.ui.components.TvButton
import com.polishmediahub.app.ui.components.TvOutlinedTextField
import com.polishmediahub.app.ui.theme.Spacing
import com.polishmediahub.app.ui.viewmodel.CustomListsViewModel

@Composable
fun CustomListsScreen(
    modifier: Modifier = Modifier,
    viewModel: CustomListsViewModel = hiltViewModel()
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle(initialValue = emptyList())
    var newListName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        Text(
            text = stringResource(id = R.string.custom_lists_title),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = Spacing.md)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            TvOutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                label = { Text(stringResource(id = R.string.new_list_name)) },
                modifier = Modifier.weight(1f)
            )
            TvButton(
                onClick = {
                    if (newListName.isNotBlank()) {
                        viewModel.createList(newListName)
                        newListName = ""
                    }
                }
            ) {
                Text(stringResource(id = R.string.add))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(lists, key = { it.listId }) { list ->
                FocusableSurface(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = list.name,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }
        }
    }
}
