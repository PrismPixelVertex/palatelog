package com.prismpixelvertex.palatelog.core.whatsnew

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import com.prismpixelvertex.palatelog.R
import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey

object WhatsNewRegistry {
    private val RELEASES = listOf(
        WhatsNewRelease(
            versionCode = 44,
            versionName = "1.2.35",
            titleRes = R.string.whats_new_title,
            subtitleRes = R.string.whats_new_1_2_35_subtitle,
            features = listOf(
                WhatsNewFeature(
                    icon = Icons.Default.Restaurant,
                    titleRes = R.string.whats_new_feature_pan_india_title,
                    descriptionRes = R.string.whats_new_feature_pan_india_desc
                ),
                WhatsNewFeature(
                    icon = Icons.Default.Refresh,
                    titleRes = R.string.whats_new_feature_search_area_title,
                    descriptionRes = R.string.whats_new_feature_search_area_desc
                )
            ),
            helpTopicKey = HelpTopicKey.MAP_EXPLORE
        ),
        WhatsNewRelease(
            versionCode = 42,
            versionName = "1.2.34",
            titleRes = R.string.whats_new_title,
            subtitleRes = R.string.whats_new_1_2_34_subtitle,
            features = listOf(
                WhatsNewFeature(
                    icon = Icons.Default.Restaurant,
                    titleRes = R.string.whats_new_feature_poi_title,
                    descriptionRes = R.string.whats_new_feature_poi_desc
                ),
                WhatsNewFeature(
                    icon = Icons.Default.Bookmark,
                    titleRes = R.string.whats_new_feature_saved_title,
                    descriptionRes = R.string.whats_new_feature_saved_desc
                ),
                WhatsNewFeature(
                    icon = Icons.Default.LocationOn,
                    titleRes = R.string.whats_new_feature_precision_title,
                    descriptionRes = R.string.whats_new_feature_precision_desc
                )
            ),
            helpTopicKey = HelpTopicKey.MAP_EXPLORE
        )
    )

    fun getReleaseForVersion(versionCode: Int): WhatsNewRelease? {
        return RELEASES.find { it.versionCode == versionCode }
    }
}
