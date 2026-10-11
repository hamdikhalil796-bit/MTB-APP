package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.MaintenanceEntry
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.ui.theme.NeumorphicCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Maintenance log: a free-form record of blade changes, tool swaps, repairs, and anything
 * else on the shop's equipment that needed fixing. Every entry is just a title/date/notes —
 * there's no fixed machine list elsewhere in the app to tie entries to.
 */
@Composable
fun MaintenanceScreen(
    entries: List<MaintenanceEntry>,
    onAddEntryClick: (title: String, date: String, notes: String) -> Unit,
    onDeleteEntryClick: (String) -> Unit,
    // Without "add and delete maintenance entries" the log is view-only.
    canEdit: Boolean = true
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var entryPendingRemoval by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredEntries = entries
        .filter { it.title.contains(searchQuery, ignoreCase = true) || it.notes.contains(searchQuery, ignoreCase = true) }
        .sortedByDescending { it.timestamp }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        NeumorphicCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 700.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = MtbWebColors.CardWhite,
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.maintenance_title),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.heading
                )
                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                MtbTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(bottom = 12.dp),
                    placeholder = stringResource(R.string.maintenance_search_placeholder)
                )

                if (filteredEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(if (entries.isEmpty()) R.string.maintenance_empty else R.string.articles_empty),
                            color = MtbWebColors.MutedText,
                            fontSize = MtbTextSizes.label
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            MaintenanceEntryCard(
                                entry = entry,
                                onDeleteClick = if (canEdit) ({ entryPendingRemoval = entry }) else null
                            )
                        }
                    }
                }

                if (canEdit) {
                    Spacer(modifier = Modifier.height(16.dp))

                    MtbButton(
                        text = stringResource(R.string.maintenance_add_button),
                        onClick = { showAddDialog = true },
                        containerColor = MtbWebColors.SecondaryBrown,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        MaintenanceEntryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, date, notes ->
                onAddEntryClick(title, date, notes)
                showAddDialog = false
            }
        )
    }

    entryPendingRemoval?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryPendingRemoval = null },
            title = { Text(stringResource(R.string.maintenance_delete_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.maintenance_delete_confirm, entry.title),
                    fontSize = MtbTextSizes.body
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEntryClick(entry.id)
                    entryPendingRemoval = null
                }) {
                    Text(stringResource(R.string.common_delete), color = MtbWebColors.DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingRemoval = null }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }
}

@Composable
private fun MaintenanceEntryCard(entry: MaintenanceEntry, onDeleteClick: (() -> Unit)?) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        containerColor = MtbWebColors.HoverBg,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    color = MtbWebColors.HeadingBrown,
                    fontSize = MtbTextSizes.subtitle
                )
                Text(
                    text = entry.date,
                    color = MtbWebColors.MutedText,
                    fontSize = MtbTextSizes.caption,
                    fontFamily = FontFamily.Monospace
                )
                if (entry.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.notes,
                        color = MtbWebColors.TextDark,
                        fontSize = MtbTextSizes.body
                    )
                }
            }
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = MtbWebColors.DeleteRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MaintenanceEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, date: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var notes by remember { mutableStateOf("") }
    val requiredFieldsMessage = stringResource(R.string.maintenance_title_required)
    var showValidation by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MtbWebColors.HeadingBrown,
        unfocusedTextColor = MtbWebColors.HeadingBrown,
        focusedBorderColor = MtbWebColors.SecondaryBrown,
        unfocusedBorderColor = MtbWebColors.BgBeige
    )

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogSurface {
            NeumorphicCard(
                shape = RoundedCornerShape(16.dp),
                containerColor = MtbWebColors.CardWhite,
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .widthIn(max = 420.dp)
                ) {
                    Text(
                        stringResource(R.string.maintenance_add_title),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.title
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(stringResource(R.string.maintenance_field_title), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; showValidation = false },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.maintenance_field_title_placeholder), fontSize = MtbTextSizes.label) },
                        singleLine = true,
                        isError = showValidation && title.isBlank(),
                        colors = textFieldColors
                    )
                    if (showValidation && title.isBlank()) {
                        Text(requiredFieldsMessage, color = MtbWebColors.DeleteRed, fontSize = MtbTextSizes.caption)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.maintenance_field_date), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(stringResource(R.string.maintenance_field_notes), color = MtbWebColors.HeadingBrown, fontSize = MtbTextSizes.label, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        placeholder = { Text(stringResource(R.string.maintenance_field_notes_placeholder), fontSize = MtbTextSizes.label) },
                        colors = textFieldColors
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MtbOutlinedButton(
                            text = stringResource(R.string.common_cancel),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        MtbButton(
                            text = stringResource(R.string.common_save),
                            onClick = {
                                if (title.isBlank()) {
                                    showValidation = true
                                } else {
                                    onConfirm(title.trim(), date.trim(), notes.trim())
                                }
                            },
                            containerColor = MtbWebColors.SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
