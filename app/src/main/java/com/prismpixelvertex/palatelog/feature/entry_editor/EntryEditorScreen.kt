package com.prismpixelvertex.palatelog.feature.entry_editor

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.prismpixelvertex.palatelog.core.designsystem.CameraXCaptureDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import com.prismpixelvertex.palatelog.core.designsystem.DishVerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.LocationPickerDialog
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmber
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictChoiceChip
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreenContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.prismpixelvertex.palatelog.core.model.Channel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Verdict
import com.prismpixelvertex.palatelog.core.util.MediaUtils
import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorScreen(
    viewModel: EntryEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHelpGuide: (HelpTopicKey?) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedToast = stringResource(R.string.entry_editor_saved_toast)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EntryEditorEvent.SavedSuccessfully -> {
                    Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is EntryEditorEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is EntryEditorEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            withDismissAction = true
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.retryResolveAddress()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        action = data.visuals.actionLabel?.let { label ->
                            {
                                TextButton(onClick = { data.performAction() }) {
                                    Text(label)
                                }
                            }
                        },
                        dismissAction = if (data.visuals.withDismissAction) {
                            {
                                TextButton(onClick = { data.dismiss() }) {
                                    Text(
                                        text = stringResource(R.string.action_ignore),
                                        color = MaterialTheme.colorScheme.inversePrimary
                                    )
                                }
                            }
                        } else null,
                        actionOnNewLine = true
                    ) {
                        Text(data.visuals.message)
                    }
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.restaurantId != null) stringResource(R.string.entry_editor_title_edit) else stringResource(R.string.entry_editor_title_new),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_close))
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToHelpGuide(HelpTopicKey.VISIT_LOG) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.help_title),
                            tint = AmberPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Channel Selector
            Text(
                text = stringResource(R.string.entry_editor_section_visit),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                uiState.availableChannels.forEach { channelName ->
                    FilterChip(
                        selected = uiState.channel.equals(channelName, ignoreCase = true),
                        onClick = { viewModel.onChannelSelected(channelName) },
                        label = { Text(Channel.getChannelDisplayName(channelName)) }
                    )
                }

                FilterChip(
                    selected = false,
                    onClick = { viewModel.showAddChannelDialog(true) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.entry_editor_add_dish))
                        }
                    }
                )
            }

            // Section 2: Restaurant Name
            OutlinedTextField(
                value = uiState.restaurantName,
                onValueChange = { viewModel.onRestaurantNameChanged(it) },
                label = { Text(stringResource(R.string.entry_editor_field_name)) },
                placeholder = { Text(stringResource(R.string.entry_editor_spot_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Location / Address Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.address,
                    onValueChange = { viewModel.onAddressChanged(it) },
                    label = { Text(stringResource(R.string.entry_editor_field_address)) },
                    placeholder = { Text(stringResource(R.string.entry_editor_address_placeholder)) },
                    trailingIcon = if (uiState.isResolvingAddress) {
                        { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = uiState.city,
                    onValueChange = { viewModel.onCityChanged(it) },
                    label = { Text(stringResource(R.string.entry_editor_field_city)) },
                    placeholder = { Text(stringResource(R.string.entry_editor_city_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            var showMapPicker by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.latitude != 0.0 || uiState.longitude != 0.0)
                            stringResource(R.string.entry_editor_pinned_coords, String.format(java.util.Locale.US, "%.4f", uiState.latitude), String.format(java.util.Locale.US, "%.4f", uiState.longitude))
                        else stringResource(R.string.entry_editor_no_location_pinned),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.addressResolutionFailed && uiState.address.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.retryResolveAddress() }
                                .padding(vertical = 2.dp)
                        ) {
                            if (uiState.isResolvingAddress) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.entry_editor_retry_address_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showMapPicker = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (uiState.latitude != 0.0 || uiState.longitude != 0.0) stringResource(R.string.entry_editor_change_on_map) else stringResource(R.string.entry_editor_pick_location_btn))
                }
            }

            if (showMapPicker) {
                LocationPickerDialog(
                    initialLatitude = uiState.latitude,
                    initialLongitude = uiState.longitude,
                    onDismiss = { showMapPicker = false },
                    onLocationConfirmed = { lat, lng ->
                        viewModel.onLocationPicked(lat, lng)
                        showMapPicker = false
                    },
                    onLocationConfirmedWithPlace = { lat, lng, placeName ->
                        viewModel.onLocationPicked(lat, lng)
                        if (!placeName.isNullOrBlank() && uiState.restaurantName.isBlank()) {
                            viewModel.onRestaurantNameChanged(placeName)
                        }
                        showMapPicker = false
                    }
                )
            }

            // Previous Dishes Chips for this restaurant
            if (uiState.previousDishes.isNotEmpty()) {
                Column {
                    Text(
                        text = stringResource(R.string.entry_editor_remembered_dishes_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AmberPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.previousDishes.distinctBy { it.dishName }.forEach { dish ->
                            val verdict = try { DishVerdict.valueOf(dish.verdict) } catch (_: Exception) { DishVerdict.GO }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.addDishDraft(
                                            dishName = dish.dishName,
                                            verdict = verdict,
                                            tags = dish.tags.joinToString(", ")
                                        )
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${dish.dishName} ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    DishVerdictBadge(verdict = verdict)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Overall Visit Verdict
            Text(
                text = stringResource(R.string.entry_editor_field_overall_verdict),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VerdictChoiceChip(
                    label = stringResource(R.string.verdict_go),
                    isSelected = uiState.overallVerdict == Verdict.GO,
                    color = VerdictGoGreen,
                    containerColor = VerdictGoGreenContainer,
                    onClick = { viewModel.onOverallVerdictSelected(Verdict.GO) },
                    modifier = Modifier.weight(1f)
                )
                VerdictChoiceChip(
                    label = stringResource(R.string.verdict_no_go),
                    isSelected = uiState.overallVerdict == Verdict.NO_GO,
                    color = VerdictNoGoRed,
                    containerColor = VerdictNoGoRedContainer,
                    onClick = { viewModel.onOverallVerdictSelected(Verdict.NO_GO) },
                    modifier = Modifier.weight(1f)
                )
                VerdictChoiceChip(
                    label = stringResource(R.string.verdict_caution),
                    isSelected = uiState.overallVerdict == Verdict.CAUTION,
                    color = VerdictCautionAmber,
                    containerColor = VerdictCautionAmberContainer,
                    onClick = { viewModel.onOverallVerdictSelected(Verdict.CAUTION) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Section 4: Dishes Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.entry_editor_section_dishes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onNavigateToHelpGuide(HelpTopicKey.DISH_MATRIX) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.restaurant_detail_dish_matrix_help_desc),
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.addDishDraft() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.entry_editor_add_dish))
                }
            }

            if (uiState.dishes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.entry_editor_no_dishes_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                uiState.dishes.forEachIndexed { index, dishDraft ->
                    DishEditorCard(
                        index = index,
                        draft = dishDraft,
                        familyMembers = uiState.familyMembers,
                        onUpdate = { updated -> viewModel.updateDishDraft(index, updated) },
                        onRemove = { viewModel.removeDishDraft(index) },
                        onPhotoAdded = { uri -> viewModel.onDishPhotoAdded(index, uri) },
                        onPhotoRemoved = { uri -> viewModel.onDishPhotoRemoved(index, uri) }
                    )
                }
            }

            // Section 5: Bill & Details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = AmberPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.entry_editor_section_notes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = uiState.billAmountText,
                        onValueChange = { viewModel.onBillAmountChanged(it) },
                        label = { Text(stringResource(R.string.entry_editor_field_bill_amount)) },
                        placeholder = { Text(stringResource(R.string.entry_editor_bill_amount_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Bill / Receipt / Menu Photo Attachment
                    Text(
                        text = stringResource(R.string.entry_editor_attach_receipt_btn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PhotoAttachmentBox(
                        photoUris = uiState.visitPhotoUris,
                        onPhotoAdded = { uri -> viewModel.onVisitPhotoAdded(uri) },
                        onPhotoRemoved = { uri -> viewModel.onVisitPhotoRemoved(uri) }
                    )

                    OutlinedTextField(
                        value = uiState.generalNotes,
                        onValueChange = { viewModel.onGeneralNotesChanged(it) },
                        label = { Text(stringResource(R.string.entry_editor_field_notes)) },
                        placeholder = { Text(stringResource(R.string.entry_editor_notes_placeholder)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 6: Shared with Family Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = AmberPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.entry_editor_share_family_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.isShared) stringResource(R.string.entry_editor_share_family_syncs) else stringResource(R.string.entry_editor_share_family_private),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.isShared,
                    onCheckedChange = { viewModel.onSharedToggle(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = { viewModel.saveEntry() },
                enabled = !uiState.isSaving && uiState.restaurantName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                shape = RoundedCornerShape(Dimens.RadiusLarge),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.ButtonHeightStandard)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.entry_editor_save_btn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Add Custom Channel Dialog
    if (uiState.showAddChannelDialog) {
        var newChannelName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.showAddChannelDialog(false) },
            title = { Text(stringResource(R.string.entry_editor_add_channel_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = newChannelName,
                    onValueChange = { newChannelName = it },
                    label = { Text(stringResource(R.string.entry_editor_add_channel_name_label)) },
                    placeholder = { Text(stringResource(R.string.entry_editor_add_channel_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newChannelName.isNotBlank()) {
                        viewModel.onAddCustomChannel(newChannelName)
                    }
                }) {
                    Text(stringResource(R.string.settings_dialog_add_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showAddChannelDialog(false) }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun PhotoAttachmentBox(
    photoUris: List<String>,
    onPhotoAdded: (String) -> Unit,
    onPhotoRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val cameraPermissionRequiredMsg = stringResource(R.string.camera_permission_required)
    val cameraPickerErrorFormat = stringResource(R.string.camera_picker_error)
    var showCameraXDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCameraXDialog = true
        } else {
            Toast.makeText(context, cameraPermissionRequiredMsg, Toast.LENGTH_SHORT).show()
        }
    }

    val pickPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val saved = MediaUtils.copyUriToInternalStorage(context, uri)
            onPhotoAdded(saved)
        }
    }

    if (showCameraXDialog) {
        CameraXCaptureDialog(
            onDismissRequest = { showCameraXDialog = false },
            onPhotoCaptured = { uri ->
                val saved = MediaUtils.copyUriToInternalStorage(context, uri)
                onPhotoAdded(saved)
            }
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        photoUris.forEach { uriString ->
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = uriString,
                    contentDescription = "Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { onPhotoRemoved(uriString) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(22.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        OutlinedButton(
            onClick = {
                try {
                    pickPhotoLauncher.launch("image/*")
                } catch (e: Exception) {
                    Toast.makeText(context, String.format(locale, cameraPickerErrorFormat, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(72.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(stringResource(R.string.entry_editor_gallery), style = MaterialTheme.typography.labelSmall)
            }
        }

        OutlinedButton(
            onClick = {
                val hasCameraPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (hasCameraPermission) {
                    showCameraXDialog = true
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(72.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(stringResource(R.string.entry_editor_camera), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DishEditorCard(
    index: Int,
    draft: DishDraft,
    familyMembers: List<com.prismpixelvertex.palatelog.core.model.FamilyMember>,
    onUpdate: (DishDraft) -> Unit,
    onRemove: () -> Unit,
    onPhotoAdded: (String) -> Unit,
    onPhotoRemoved: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.entry_editor_dish_index_header, index + 1),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = draft.name,
                onValueChange = { onUpdate(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.entry_editor_dish_name)) },
                placeholder = { Text(stringResource(R.string.entry_editor_dish_name_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Dish Verdict selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DishVerdict.entries.forEach { v ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (draft.verdict == v) {
                                    when (v) {
                                        DishVerdict.GO -> VerdictGoGreenContainer
                                        DishVerdict.NO_GO -> VerdictNoGoRedContainer
                                        DishVerdict.MEH -> VerdictCautionAmberContainer
                                    }
                                } else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onUpdate(draft.copy(verdict = v)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = v.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (draft.verdict == v) {
                                when (v) {
                                    DishVerdict.GO -> VerdictGoGreen
                                    DishVerdict.NO_GO -> VerdictNoGoRed
                                    DishVerdict.MEH -> VerdictCautionAmber
                                }
                            } else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedTextField(
                value = draft.tagsText,
                onValueChange = { onUpdate(draft.copy(tagsText = it)) },
                label = { Text(stringResource(R.string.entry_editor_dish_tags_label)) },
                placeholder = { Text(stringResource(R.string.entry_editor_dish_tags_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Family Member Reaction Input
            OutlinedTextField(
                value = draft.memberReaction,
                onValueChange = { onUpdate(draft.copy(memberReaction = it)) },
                label = { Text(stringResource(R.string.entry_editor_dish_family_notes_label)) },
                placeholder = { Text(stringResource(R.string.entry_editor_dish_family_notes_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Dish Photos Attachment
            Text(
                text = stringResource(R.string.entry_editor_dish_photos_header),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PhotoAttachmentBox(
                photoUris = draft.photoUris,
                onPhotoAdded = onPhotoAdded,
                onPhotoRemoved = onPhotoRemoved
            )
        }
    }
}
