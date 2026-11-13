package com.prismpixelvertex.palatelog.core.whatsnew

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey

data class WhatsNewFeature(
    val icon: ImageVector,
    @get:StringRes val titleRes: Int,
    @get:StringRes val descriptionRes: Int
)

data class WhatsNewRelease(
    val versionCode: Int,
    val versionName: String,
    @get:StringRes val titleRes: Int,
    @get:StringRes val subtitleRes: Int? = null,
    val features: List<WhatsNewFeature>,
    val helpTopicKey: HelpTopicKey? = null
)
