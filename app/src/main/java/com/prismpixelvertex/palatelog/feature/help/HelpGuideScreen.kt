package com.prismpixelvertex.palatelog.feature.help

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.core.designsystem.AmberPrimary
import com.prismpixelvertex.palatelog.core.designsystem.ChannelBadge
import com.prismpixelvertex.palatelog.core.designsystem.Dimens
import com.prismpixelvertex.palatelog.core.designsystem.DishVerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictBadge
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmberContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictCautionAmberText
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreen
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreenContainer
import com.prismpixelvertex.palatelog.core.designsystem.VerdictGoGreenText
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRed
import com.prismpixelvertex.palatelog.core.designsystem.VerdictNoGoRedContainer
import com.prismpixelvertex.palatelog.core.model.DishVerdict
import com.prismpixelvertex.palatelog.core.model.Verdict
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class HelpSectionItem(
    val key: HelpTopicKey,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val categoryTagRes: Int,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBgColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpGuideScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialTopic: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val canScrollForward by remember { derivedStateOf { listState.canScrollForward } }

    val initialTopicKey = remember(initialTopic) { HelpTopicKey.fromId(initialTopic) }

    val sections = remember {
        listOf(
            HelpSectionItem(
                key = HelpTopicKey.DISH_MATRIX,
                titleRes = R.string.help_card_dish_matrix_title,
                subtitleRes = R.string.help_card_dish_matrix_subtitle,
                categoryTagRes = R.string.help_tag_dish_matrix,
                icon = Icons.Default.DinnerDining,
                iconColor = Color(0xFF8B5CF6),
                iconBgColor = Color(0xFF8B5CF6).copy(alpha = 0.15f)
            ),
            HelpSectionItem(
                key = HelpTopicKey.QUICK_LOG,
                titleRes = R.string.help_card_quick_log_title,
                subtitleRes = R.string.help_card_quick_log_subtitle,
                categoryTagRes = R.string.help_tag_quick_log,
                icon = Icons.Default.FlashOn,
                iconColor = VerdictNoGoRed,
                iconBgColor = VerdictNoGoRedContainer
            ),
            HelpSectionItem(
                key = HelpTopicKey.VISIT_LOG,
                titleRes = R.string.help_card_visit_log_title,
                subtitleRes = R.string.help_card_visit_log_subtitle,
                categoryTagRes = R.string.help_tag_visit_log,
                icon = Icons.Default.Restaurant,
                iconColor = VerdictGoGreen,
                iconBgColor = VerdictGoGreenContainer
            ),
            HelpSectionItem(
                key = HelpTopicKey.SYNC,
                titleRes = R.string.help_card_sync_title,
                subtitleRes = R.string.help_card_sync_subtitle,
                categoryTagRes = R.string.help_tag_sync,
                icon = Icons.Default.CloudSync,
                iconColor = AmberPrimary,
                iconBgColor = AmberPrimary.copy(alpha = 0.15f)
            ),
            HelpSectionItem(
                key = HelpTopicKey.MAP_EXPLORE,
                titleRes = R.string.help_card_map_title,
                subtitleRes = R.string.help_card_map_subtitle,
                categoryTagRes = R.string.help_tag_map,
                icon = Icons.Default.Map,
                iconColor = Color(0xFF0EA5E9),
                iconBgColor = Color(0xFF0EA5E9).copy(alpha = 0.15f)
            ),
            HelpSectionItem(
                key = HelpTopicKey.PRIVACY,
                titleRes = R.string.help_card_privacy_title,
                subtitleRes = R.string.help_card_privacy_subtitle,
                categoryTagRes = R.string.help_tag_privacy,
                icon = Icons.Default.Lock,
                iconColor = Color(0xFF64748B),
                iconBgColor = Color(0xFF64748B).copy(alpha = 0.15f)
            ),
            HelpSectionItem(
                key = HelpTopicKey.RADAR,
                titleRes = R.string.help_card_radar_title,
                subtitleRes = R.string.help_card_radar_subtitle,
                categoryTagRes = R.string.help_tag_radar,
                icon = Icons.Default.Radar,
                iconColor = Color(0xFF0284C7),
                iconBgColor = Color(0xFF0284C7).copy(alpha = 0.15f)
            )
        )
    }

    var expandedKeys by remember {
        mutableStateOf(
            if (initialTopicKey != null) setOf(initialTopicKey)
            else setOf(HelpTopicKey.DISH_MATRIX, HelpTopicKey.QUICK_LOG)
        )
    }

    var selectedFilterKey by remember { mutableStateOf(initialTopicKey) }
    var highlightedKey by remember { mutableStateOf(initialTopicKey) }

    LaunchedEffect(initialTopicKey) {
        if (initialTopicKey != null) {
            val targetIndex = sections.indexOfFirst { it.key == initialTopicKey }
            if (targetIndex >= 0) {
                expandedKeys = expandedKeys + initialTopicKey
                selectedFilterKey = initialTopicKey
                delay(150.milliseconds)
                listState.animateScrollToItem(targetIndex)
                delay(2000.milliseconds)
                highlightedKey = null
            }
        }
    }

    fun toggleCard(key: HelpTopicKey) {
        expandedKeys = if (expandedKeys.contains(key)) {
            expandedKeys - key
        } else {
            expandedKeys + key
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.help_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.help_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_close)
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
        ) {
            // Horizontal Jump / Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.SpacingDefault, vertical = Dimens.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
            ) {
                FilterChip(
                    selected = selectedFilterKey == null,
                    onClick = {
                        selectedFilterKey = null
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    label = { Text(stringResource(R.string.help_filter_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = Color.White
                    )
                )

                HelpTopicKey.entries.forEach { topicKey ->
                    FilterChip(
                        selected = selectedFilterKey == topicKey,
                        onClick = {
                            selectedFilterKey = topicKey
                            expandedKeys = expandedKeys + topicKey
                            val index = sections.indexOfFirst { it.key == topicKey }
                            if (index >= 0) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                        label = { Text(stringResource(topicKey.chipLabelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = Dimens.SpacingDefault,
                        top = Dimens.SpacingDefault,
                        end = Dimens.SpacingDefault,
                        bottom = Dimens.SpacingLarge
                    ),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingDefault),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(sections, key = { _, item -> item.key.name }) { _, section ->
                        val isExpanded = expandedKeys.contains(section.key)
                        val isHighlighted = highlightedKey == section.key

                        val borderColor by animateColorAsState(
                            targetValue = if (isHighlighted) AmberPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            label = "cardBorder"
                        )

                        Card(
                            shape = RoundedCornerShape(Dimens.RadiusLarge),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(if (isHighlighted) Dimens.BorderThick else Dimens.BorderThin, borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Clickable Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { toggleCard(section.key) }
                                        .padding(Dimens.SpacingDefault),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.HelpIconContainerSize)
                                            .clip(CircleShape)
                                            .background(section.iconBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = section.icon,
                                            contentDescription = null,
                                            tint = section.iconColor,
                                            modifier = Modifier.size(Dimens.IconSizeDefault)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(Dimens.SpacingStandard))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(section.categoryTagRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = section.iconColor
                                        )
                                        Text(
                                            text = stringResource(section.titleRes),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(section.subtitleRes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val rotationAngle by animateFloatAsState(
                                        targetValue = if (isExpanded) 180f else 0f,
                                        label = "chevronRotation"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = stringResource(if (isExpanded) R.string.help_action_collapse else R.string.help_action_expand),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.rotate(rotationAngle)
                                    )
                                }

                                // Expandable Body
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = Dimens.SpacingDefault, end = Dimens.SpacingDefault, bottom = Dimens.SpacingDefault),
                                        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingStandard)
                                    ) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )

                                        when (section.key) {
                                            HelpTopicKey.DISH_MATRIX -> DishMatrixTopicContent()
                                            HelpTopicKey.QUICK_LOG -> QuickLogTopicContent()
                                            HelpTopicKey.VISIT_LOG -> VisitLogTopicContent()
                                            HelpTopicKey.SYNC -> SyncTopicContent()
                                            HelpTopicKey.MAP_EXPLORE -> MapExploreTopicContent()
                                            HelpTopicKey.PRIVACY -> PrivacyTopicContent()
                                            HelpTopicKey.RADAR -> RadarTopicContent()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floating indicator showing more cards below
                androidx.compose.animation.AnimatedVisibility(
                    visible = canScrollForward,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Dimens.SpacingMedium)
                ) {
                    Surface(
                        onClick = {
                            coroutineScope.launch {
                                val nextIndex = (listState.firstVisibleItemIndex + 1).coerceAtMost(sections.lastIndex)
                                listState.animateScrollToItem(nextIndex)
                            }
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = Dimens.ElevationMedium,
                        shadowElevation = Dimens.ElevationMedium,
                        border = BorderStroke(Dimens.BorderThin, AmberPrimary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = Dimens.SpacingMedium,
                                vertical = Dimens.SpacingSmall
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.help_scroll_more_cards),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(Dimens.IconSizeSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 1. DISH MATRIX TOPIC CONTENT (Core Philosophy)
// ---------------------------------------------------------------------------
@Composable
private fun DishMatrixTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_dish_matrix_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        // Visual Example Card
        Card(
            shape = RoundedCornerShape(Dimens.RadiusStandard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(Dimens.SpacingStandard), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.help_dish_matrix_example_label), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.help_dish_matrix_sample_restaurant_name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                    VerdictBadge(Verdict.GO)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Dish 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.help_dish_matrix_sample_dish_1_name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.help_dish_matrix_sample_dish_1_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DishVerdictBadge(DishVerdict.GO)
                }

                // Dish 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.help_dish_matrix_sample_dish_2_name), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.help_dish_matrix_sample_dish_2_desc), style = MaterialTheme.typography.labelSmall, color = VerdictNoGoRed)
                    }
                    DishVerdictBadge(DishVerdict.NO_GO)
                }
            }
        }

        Text(
            text = stringResource(R.string.help_dish_matrix_points),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// 2. QUICK LOG TOPIC CONTENT (Speed Capture)
// ---------------------------------------------------------------------------
@Composable
private fun QuickLogTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_quick_log_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = VerdictNoGoRed, modifier = Modifier.size(Dimens.IconSizeSmall))
                Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                Text(stringResource(R.string.help_quick_log_steps_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Text(stringResource(R.string.help_quick_log_step_1), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_quick_log_step_2), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_quick_log_step_3), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
                VerdictBadge(Verdict.NO_GO)
                VerdictBadge(Verdict.GO)
                VerdictBadge(Verdict.CAUTION)
            }
            Text(stringResource(R.string.help_quick_log_step_4), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_quick_log_step_5), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusSmall))
                .background(VerdictGoGreenContainer)
                .padding(Dimens.SpacingMedium)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = VerdictGoGreenText,
                    modifier = Modifier.size(Dimens.IconSizeSmall).padding(top = Dimens.SpacingXXSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                Text(
                    text = stringResource(R.string.help_quick_log_reuse_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = VerdictGoGreenText
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. VISIT LOG TOPIC CONTENT (Visits & Orders)
// ---------------------------------------------------------------------------
@Composable
private fun VisitLogTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_visit_log_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Text(stringResource(R.string.help_visit_log_record_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)
            ) {
                ChannelBadge("DINE_IN")
                ChannelBadge("ZOMATO")
                ChannelBadge("SWIGGY")
                ChannelBadge("HIGHWAY_STOP")
                ChannelBadge("TAKEAWAY")
            }
            Text(stringResource(R.string.help_visit_log_point_channel), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_visit_log_point_bill), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_visit_log_point_dishes), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_visit_log_point_attribution), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// 4. SYNC TOPIC CONTENT (Family Cloud Sync)
// ---------------------------------------------------------------------------
@Composable
private fun SyncTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingStandard)) {
        Text(
            text = stringResource(R.string.help_sync_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        // Comparison Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusStandard))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            // Mode A Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.SpacingStandard), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.help_sync_mode_a_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = AmberPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.RadiusSmall))
                                .background(VerdictGoGreenContainer)
                                .padding(horizontal = Dimens.SpacingSmall, vertical = Dimens.SpacingXXSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.help_sync_mode_a_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VerdictGoGreenText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.help_sync_mode_a_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Mode B Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(Dimens.RadiusMedium),
                border = BorderStroke(Dimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimens.SpacingStandard), verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.help_sync_mode_b_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = AmberPrimary,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(Dimens.RadiusSmall))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                .padding(horizontal = Dimens.SpacingSmall, vertical = Dimens.SpacingXXSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.help_sync_mode_b_badge),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5CF6),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.help_sync_mode_b_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // WhatsApp Recovery Tip Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusSmall))
                .background(VerdictCautionAmberContainer)
                .padding(Dimens.SpacingMedium)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VerdictCautionAmberText,
                    modifier = Modifier.size(Dimens.IconSizeSmall).padding(top = Dimens.SpacingXXSmall)
                )
                Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
                Text(
                    text = stringResource(R.string.help_sync_whatsapp_tip),
                    style = MaterialTheme.typography.bodySmall,
                    color = VerdictCautionAmberText
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. MAP EXPLORE TOPIC CONTENT (Travel & Maps)
// ---------------------------------------------------------------------------
@Composable
private fun MapExploreTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_map_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        // Color-Coded Travel Markers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Text(stringResource(R.string.help_map_pins_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.help_map_pin_green), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_pin_red), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_pin_amber), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_nav_point), style = MaterialTheme.typography.bodySmall)
        }

        // Map Marker Types & Visual Indicators
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Text(stringResource(R.string.help_map_layers_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.help_map_saved_layer), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_poi_layer), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_custom_pin), style = MaterialTheme.typography.bodySmall)
        }

        // Filter Chips & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Text(stringResource(R.string.help_map_filters_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.help_map_filter_saved), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_filter_pois), style = MaterialTheme.typography.bodySmall)
        }

        // Precision Taps & Anti-Clutter Rules
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Text(stringResource(R.string.help_map_gestures_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.help_map_gesture_zoom), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_gesture_precision), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_gesture_unhook), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_map_gesture_pan), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ---------------------------------------------------------------------------
// 6. PRIVACY TOPIC CONTENT (Security & Storage)
// ---------------------------------------------------------------------------
@Composable
private fun PrivacyTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_privacy_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.RadiusMedium))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(Dimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(Dimens.IconSizeSmall))
                Spacer(modifier = Modifier.width(Dimens.SpacingSmall))
                Text(stringResource(R.string.help_privacy_switch_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Text(stringResource(R.string.help_privacy_points), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_privacy_conflict_point), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.help_privacy_offline_point), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// 7. PALATE RADAR TOPIC CONTENT (Highway Proximity & Geofencing)
@Composable
private fun RadarTopicContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium)) {
        Text(
            text = stringResource(R.string.help_radar_intro),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = stringResource(R.string.help_radar_points),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
