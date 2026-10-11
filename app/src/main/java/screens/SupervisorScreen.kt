package com.mtb.mtb.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mtb.mtb.R
import com.mtb.mtb.app.MtbTextSizes
import com.mtb.mtb.app.MtbWebColors
import com.mtb.mtb.app.Permissions
import com.mtb.mtb.app.Profile
import com.mtb.mtb.app.ProfileError
import com.mtb.mtb.app.ProfileRepository
import com.mtb.mtb.ui.theme.NeumorphicCard

private val PERMISSION_LABELS = mapOf(
    "newOrder" to R.string.perm_new_order,
    "currentOrder" to R.string.perm_current_order,
    "history" to R.string.perm_history,
    "articles" to R.string.perm_articles,
    "stock" to R.string.perm_stock,
    "maintenance" to R.string.perm_maintenance,
    "statistics" to R.string.perm_statistics,
    "editOrders" to R.string.perm_edit_orders,
    "prepareOrders" to R.string.perm_prepare_orders,
    "editArticles" to R.string.perm_edit_articles,
    "editStock" to R.string.perm_edit_stock,
    "editMaintenance" to R.string.perm_edit_maintenance
)

private sealed interface Editing {
    data object Guest : Editing
    data object New : Editing
    data class Existing(val profile: Profile) : Editing
}

/**
 * Supervisor-only: create profiles and choose what each one (and the guest login) can see and
 * do. Same screen and Firestore data as the website's SupervisorScreen.tsx.
 */
@Composable
fun SupervisorScreen(
    currentUsername: String?,
    showMessage: (String) -> Unit,
    // Supervisors have no Home or bottom bar, so settings (and logout) are reached from here.
    onSettingsClick: () -> Unit
) {
    var profiles by remember { mutableStateOf<List<Profile>?>(null) }
    var guestPermissions by remember { mutableStateOf(Permissions.DEFAULT_GUEST) }
    var editing by remember { mutableStateOf<Editing?>(null) }

    DisposableEffect(Unit) {
        val profilesListener = ProfileRepository.listenProfiles { profiles = it }
        val guestListener = ProfileRepository.listenGuestPermissions { guestPermissions = it }
        onDispose {
            profilesListener.remove()
            guestListener.remove()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MtbWebColors.BgPage)
            .padding(8.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.supervisor_title),
                        color = MtbWebColors.HeadingBrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.heading,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MtbWebColors.HeadingBrown)
                    }
                }
                HorizontalDivider(color = MtbWebColors.BgBeige, thickness = 2.dp)
                Spacer(modifier = Modifier.height(12.dp))

                SupervisorRow(
                    title = stringResource(R.string.supervisor_guest_title),
                    subtitle = stringResource(R.string.supervisor_guest_hint),
                    background = MtbWebColors.BgBeige,
                    onEdit = { editing = Editing.Guest }
                )

                Spacer(modifier = Modifier.height(14.dp))

                val list = profiles
                if (list != null && list.isEmpty()) {
                    Text(
                        stringResource(R.string.supervisor_empty),
                        color = MtbWebColors.MutedText,
                        fontSize = MtbTextSizes.label,
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    )
                } else if (list != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(list, key = { it.username }) { profile ->
                            SupervisorRow(
                                title = profile.displayName.ifBlank { profile.username },
                                subtitle = "@${profile.username}",
                                background = MtbWebColors.HoverBg,
                                badges = buildList<Pair<String, Color>> {
                                    if (profile.isSupervisor) add(stringResource(R.string.supervisor_badge_supervisor) to MtbWebColors.PrimaryBrown)
                                    if (!profile.active) add(stringResource(R.string.supervisor_badge_disabled) to MtbWebColors.DeleteRed)
                                    if (profile.username == currentUsername) add(stringResource(R.string.supervisor_badge_you) to MtbWebColors.SuccessGreen)
                                },
                                dimmed = !profile.active,
                                onEdit = { editing = Editing.Existing(profile) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                MtbButton(
                    text = stringResource(R.string.supervisor_add_profile),
                    onClick = { editing = Editing.New },
                    containerColor = MtbWebColors.SecondaryBrown,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    when (val current = editing) {
        Editing.Guest -> GuestPermissionsDialog(
            initial = guestPermissions,
            onDismiss = { editing = null },
            onSaved = { message ->
                showMessage(message)
                editing = null
            }
        )
        Editing.New -> ProfileEditorDialog(
            profile = null,
            isSelf = false,
            onDismiss = { editing = null },
            onDone = { message ->
                showMessage(message)
                editing = null
            }
        )
        is Editing.Existing -> ProfileEditorDialog(
            profile = current.profile,
            isSelf = current.profile.username == currentUsername,
            onDismiss = { editing = null },
            onDone = { message ->
                showMessage(message)
                editing = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun SupervisorRow(
    title: String,
    subtitle: String,
    background: Color,
    onEdit: () -> Unit,
    badges: List<Pair<String, Color>> = emptyList(),
    dimmed: Boolean = false
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MtbWebColors.BorderGray, shape)
            .background(background, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = if (dimmed) MtbWebColors.MutedText else MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                badges.forEach { (label, color) ->
                    Text(
                        label,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = MtbTextSizes.caption,
                        maxLines = 1,
                        modifier = Modifier
                            .background(color, RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            Text(subtitle, color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label)
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = MtbWebColors.HeadingBrown, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PermissionChecklist(
    value: Permissions,
    onChange: (Permissions) -> Unit
) {
    @Composable
    fun section(titleRes: Int, keys: List<String>) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .border(1.dp, MtbWebColors.BorderGray, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                stringResource(titleRes),
                color = MtbWebColors.SecondaryBrown,
                fontWeight = FontWeight.Bold,
                fontSize = MtbTextSizes.label,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
            keys.forEach { key ->
                CheckRow(
                    label = stringResource(PERMISSION_LABELS.getValue(key)),
                    checked = value[key],
                    onCheckedChange = { onChange(value.with(key, it)) }
                )
            }
        }
    }
    section(R.string.supervisor_section_pages, Permissions.PAGE_KEYS)
    section(R.string.supervisor_section_actions, Permissions.ACTION_KEYS)
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true, strong: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = MtbWebColors.PrimaryBrown),
            modifier = Modifier.size(36.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            color = if (enabled) MtbWebColors.TextDark else MtbWebColors.MutedText,
            fontSize = MtbTextSizes.body,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun DialogCard(content: @Composable ColumnScope.() -> Unit) {
    NeumorphicCard(
        shape = RoundedCornerShape(16.dp),
        containerColor = MtbWebColors.CardWhite,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
private fun DialogButtons(busy: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MtbOutlinedButton(
            text = stringResource(R.string.common_cancel),
            onClick = { if (!busy) onCancel() },
            modifier = Modifier.weight(1f)
        )
        MtbButton(
            text = stringResource(if (busy) R.string.common_saving else R.string.common_save),
            onClick = onSave,
            containerColor = MtbWebColors.SecondaryBrown,
            enabled = !busy,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun profileErrorText(): (ProfileError) -> String {
    val texts = mapOf(
        ProfileError.USERNAME_INVALID to stringResource(R.string.error_username_invalid),
        ProfileError.USERNAME_TAKEN to stringResource(R.string.error_username_taken),
        ProfileError.PASSWORD_SHORT to stringResource(R.string.error_password_short),
        ProfileError.NETWORK to stringResource(R.string.login_error_network)
    )
    val fallback = stringResource(R.string.error_with_message, "")
    return { error -> texts[error] ?: fallback }
}

@Composable
private fun GuestPermissionsDialog(initial: Permissions, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    var perms by remember { mutableStateOf(initial) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val savedMessage = stringResource(R.string.msg_guest_saved)
    val errorText = profileErrorText()

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        AnimatedDialogSurface {
            DialogCard {
                Text(stringResource(R.string.supervisor_guest_title), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold, fontSize = MtbTextSizes.heading)
                Text(stringResource(R.string.supervisor_guest_hint), color = MtbWebColors.MutedText, fontSize = MtbTextSizes.label)
                PermissionChecklist(value = perms, onChange = { perms = it })
                if (error.isNotEmpty()) {
                    Text(error, color = MtbWebColors.DeleteRed, fontSize = MtbTextSizes.body, modifier = Modifier.padding(top = 8.dp))
                }
                DialogButtons(busy = busy, onCancel = onDismiss, onSave = {
                    busy = true
                    ProfileRepository.saveGuestPermissions(perms) { e ->
                        busy = false
                        if (e == null) onSaved(savedMessage) else error = errorText(e)
                    }
                })
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: Profile?,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onDone: (String) -> Unit
) {
    val isNew = profile == null
    var displayName by remember { mutableStateOf(profile?.displayName ?: "") }
    var username by remember { mutableStateOf(profile?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var isSupervisor by remember { mutableStateOf(profile?.isSupervisor ?: false) }
    var active by remember { mutableStateOf(profile?.active ?: true) }
    var perms by remember { mutableStateOf(profile?.permissions ?: Permissions()) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val errorText = profileErrorText()
    val nameRequired = stringResource(R.string.error_name_required)
    val passwordShort = stringResource(R.string.error_password_short)
    val msgCreated = stringResource(R.string.msg_profile_created)
    val msgSaved = stringResource(R.string.msg_profile_saved)
    val msgDeleted = stringResource(R.string.msg_profile_deleted)
    val msgPasswordReset = stringResource(R.string.msg_password_reset)

    fun finish(successMessage: String): (ProfileError?) -> Unit = { e ->
        if (e == null) {
            onDone(successMessage)
        } else {
            busy = false
            error = errorText(e)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MtbWebColors.TextDark,
        unfocusedTextColor = MtbWebColors.TextDark,
        focusedBorderColor = MtbWebColors.SecondaryBrown,
        unfocusedBorderColor = MtbWebColors.BorderGray
    )

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        AnimatedDialogSurface {
            DialogCard {
                Text(
                    stringResource(if (isNew) R.string.supervisor_new_profile else R.string.supervisor_edit_profile),
                    color = MtbWebColors.HeadingBrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = MtbTextSizes.heading
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(stringResource(R.string.supervisor_field_name)) },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.lowercase() },
                    label = { Text(stringResource(R.string.supervisor_field_username)) },
                    singleLine = true,
                    enabled = isNew,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isNew) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.supervisor_field_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // A supervisor can't demote or disable themselves, so there's always one left.
                if (!isSelf) {
                    Spacer(Modifier.height(8.dp))
                    CheckRow(
                        label = stringResource(R.string.supervisor_is_supervisor),
                        checked = isSupervisor,
                        onCheckedChange = { isSupervisor = it },
                        strong = true
                    )
                    if (!isNew) {
                        CheckRow(
                            label = stringResource(R.string.supervisor_active),
                            checked = active,
                            onCheckedChange = { active = it },
                            strong = true
                        )
                    }
                }

                // A supervisor only manages profiles, so page/action permissions don't apply.
                if (!isSupervisor) {
                    PermissionChecklist(value = perms, onChange = { perms = it })
                }

                if (error.isNotEmpty()) {
                    Text(error, color = MtbWebColors.DeleteRed, fontSize = MtbTextSizes.body, modifier = Modifier.padding(top = 8.dp))
                }

                DialogButtons(busy = busy, onCancel = onDismiss, onSave = {
                    error = ""
                    when {
                        displayName.isBlank() -> error = nameRequired
                        profile == null -> {
                            if (password.length < ProfileRepository.MIN_PASSWORD_LENGTH) {
                                error = passwordShort
                            } else {
                                busy = true
                                ProfileRepository.createProfile(username, password, displayName, isSupervisor, perms, finish(msgCreated))
                            }
                        }
                        else -> {
                            busy = true
                            ProfileRepository.updateProfile(profile, displayName, isSupervisor, active, perms, finish(msgSaved))
                        }
                    }
                })

                if (profile != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { error = ""; showReset = true }, enabled = !busy) {
                            Text(stringResource(R.string.supervisor_reset_password), color = MtbWebColors.SecondaryBrown)
                        }
                        // A supervisor can't delete their own profile, so one always remains.
                        if (!isSelf) {
                            TextButton(onClick = { error = ""; showDelete = true }, enabled = !busy) {
                                Text(stringResource(R.string.supervisor_delete_profile), color = MtbWebColors.DeleteRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReset && profile != null) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!busy) showReset = false },
            title = { Text(stringResource(R.string.supervisor_reset_password), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.supervisor_reset_hint), fontSize = MtbTextSizes.body)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.supervisor_new_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MtbWebColors.DeleteRed, fontSize = MtbTextSizes.body, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    if (newPassword.length < ProfileRepository.MIN_PASSWORD_LENGTH) {
                        error = passwordShort
                    } else {
                        busy = true
                        ProfileRepository.resetPassword(profile, newPassword, finish(msgPasswordReset))
                    }
                }) {
                    Text(stringResource(R.string.common_save), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showReset = false }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }

    if (showDelete && profile != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) showDelete = false },
            title = { Text(stringResource(R.string.supervisor_delete_profile), color = MtbWebColors.HeadingBrown, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.supervisor_delete_confirm, profile.displayName.ifBlank { profile.username }),
                        fontSize = MtbTextSizes.body
                    )
                    if (error.isNotEmpty()) {
                        Text(error, color = MtbWebColors.DeleteRed, fontSize = MtbTextSizes.body, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    busy = true
                    ProfileRepository.deleteProfile(profile, finish(msgDeleted))
                }) {
                    Text(stringResource(R.string.common_delete), color = MtbWebColors.DeleteRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showDelete = false }) {
                    Text(stringResource(R.string.common_cancel), color = MtbWebColors.HeadingBrown)
                }
            },
            containerColor = MtbWebColors.CardWhite
        )
    }
}
