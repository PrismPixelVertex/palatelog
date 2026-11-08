package com.prismpixelvertex.palatelog.feature.help

import androidx.annotation.StringRes
import com.prismpixelvertex.palatelog.R

enum class HelpTopicKey(val id: String, @StringRes val chipLabelRes: Int) {
    DISH_MATRIX("dish_matrix", R.string.help_chip_dish_matrix),
    QUICK_LOG("quick_log", R.string.help_chip_quick_log),
    VISIT_LOG("visit_log", R.string.help_chip_visit_log),
    SYNC("sync", R.string.help_chip_sync),
    MAP_EXPLORE("map_explore", R.string.help_chip_map),
    PRIVACY("privacy", R.string.help_chip_privacy),
    RADAR("radar", R.string.help_chip_radar);

    companion object {
        fun fromId(id: String?): HelpTopicKey? {
            if (id.isNullOrBlank()) return null
            return entries.find { it.id.equals(id, ignoreCase = true) }
        }
    }
}
