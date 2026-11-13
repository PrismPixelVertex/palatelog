package com.prismpixelvertex.palatelog.core.whatsnew

import com.prismpixelvertex.palatelog.feature.help.HelpTopicKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewRegistryTest {

    @Test
    fun testVersion42RegisteredCorrectly() {
        val release = WhatsNewRegistry.getReleaseForVersion(42)
        assertNotNull("Version 42 should have a registered release showcase", release)
        assertEquals(42, release!!.versionCode)
        assertEquals("1.2.34", release.versionName)
        assertEquals(HelpTopicKey.MAP_EXPLORE, release.helpTopicKey)
        assertTrue("Version 42 must contain highlight features", release.features.isNotEmpty())
        assertEquals(3, release.features.size)
    }

    @Test
    fun testUnregisteredVersionReturnsNull() {
        val release = WhatsNewRegistry.getReleaseForVersion(999)
        assertNull("Unregistered version codes should return null to suppress dialog", release)
    }

    @Test
    fun testExtensibleGuideRedirectionContract() {
        // Models support null helpTopicKey for releases without help cards
        val releaseWithoutGuide = WhatsNewRelease(
            versionCode = 100,
            versionName = "1.3.0",
            titleRes = 0,
            features = emptyList(),
            helpTopicKey = null
        )
        assertNull(releaseWithoutGuide.helpTopicKey)

        val releaseWithGuide = WhatsNewRelease(
            versionCode = 101,
            versionName = "1.3.1",
            titleRes = 0,
            features = emptyList(),
            helpTopicKey = HelpTopicKey.MAP_EXPLORE
        )
        assertEquals(HelpTopicKey.MAP_EXPLORE, releaseWithGuide.helpTopicKey)
    }
}
