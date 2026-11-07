package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.manager.StabilityManager
import com.example.model.GamingProfileType
import com.example.model.ProfileConfig
import com.example.monitor.RamMonitor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Nexus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus", appName)
    }

    @Test
    fun `verify gaming profile configurations`() {
        val perf = ProfileConfig.forType(GamingProfileType.PERFORMANCE_FOCUS)
        assertEquals(5000L, perf.pollingIntervalMs)
        assertEquals(false, perf.animationsEnabled)

        val balanced = ProfileConfig.forType(GamingProfileType.BALANCED)
        assertEquals(2500L, balanced.pollingIntervalMs)
        assertEquals(true, balanced.animationsEnabled)

        val saver = ProfileConfig.forType(GamingProfileType.BATTERY_SAVER)
        assertEquals(8000L, saver.pollingIntervalMs)
        assertEquals(false, saver.animationsEnabled)
    }

    @Test
    fun `verify ram monitor reads non-negative memory`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ramMonitor = RamMonitor(context)
        val ram = ramMonitor.getRamInfo()
        assertTrue("Total RAM should be >= 0", ram.totalBytes >= 0L)
        assertTrue("Available RAM should be >= 0", ram.availableBytes >= 0L)
    }

    @Test
    fun `verify stability health report generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stabilityManager = StabilityManager(context)
        val report = stabilityManager.getHealthReport()
        assertNotNull(report)
        assertTrue(report.stabilityScorePercent in 0..100)
        assertTrue(report.recommendations.isNotEmpty())
    }

    @Test
    fun `verify social app status resolution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcher = com.example.manager.ApplicationLauncher(context)
        val fb = launcher.getFacebookStatus()
        val msg = launcher.getMessengerStatus()
        val insta = launcher.getInstagramStatus()

        assertNotNull(fb)
        assertNotNull(msg)
        assertNotNull(insta)
        assertEquals("https://m.facebook.com", fb.webFallbackUrl)
        assertEquals("https://www.messenger.com", msg.webFallbackUrl)
        assertEquals("https://www.instagram.com", insta.webFallbackUrl)
    }
}
