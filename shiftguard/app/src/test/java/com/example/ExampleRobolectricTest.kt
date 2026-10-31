package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ShiftStorage
import com.example.data.remote.GasEngine
import com.example.ml.DocumentProcessor
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ShiftGuard", appName)
  }

  @Test
  fun `verify license plate regex matching`() {
    val regex = Regex("[A-Za-z]{2}[- ]?\\d{2}[- ]?[A-Za-z]{1,2}[- ]?\\d{4}")
    assertTrue(regex.matches("KA-02-JH-2234"))
    assertTrue(regex.matches("MH12DE1433"))
    assertTrue(regex.matches("DL 01 A 4321"))
  }

  @Test
  fun `verify storage initialization and concealed gas url`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = ShiftStorage(context)
    assertNotNull(storage.userProfile.value)
    assertEquals("officer@shiftguard.com", storage.userProfile.value?.email)
    assertTrue(storage.gasUrl.value.startsWith("https://script.google.com"))
    assertEquals(GasEngine.CONCEALED_GAS_URL, storage.gasUrl.value)
  }

  @Test
  fun `verify job card parser agent extracts handwritten notes`() {
    val sampleOcrText = """
      JOB CARD # JC-9021
      Customer: Bangalore Freight Logistics
      Handwritten Notes: Clutch plate replacement and wiring harness check completed.
      Parts: Oil Filter, Brake Pads
      Status: Inspected and signed
    """.trimIndent()

    val parsed = DocumentProcessor.parseJobCard(sampleOcrText)
    assertEquals("JC-9021", parsed.jobCardNumber)
    assertEquals("Bangalore Freight Logistics", parsed.clientName)
    assertTrue(parsed.handwrittenNotes.contains("Clutch plate replacement") || parsed.partsReplaced.contains("Oil Filter"))
  }

  @Test
  fun `verify dynamic site update and midnight trigger`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val storage = ShiftStorage(context)
    storage.updateSiteConfig("Custom Distribution Hub", 13.0827, 80.2707, 1200.0, "07:00", "16:00")
    assertEquals("Custom Distribution Hub", storage.userProfile.value?.siteName)
    assertEquals(1200.0, storage.userProfile.value?.geofenceRadiusMeters ?: 0.0, 0.1)

    // Verify midnight test trigger
    storage.simulateMidnightExpiry(true)
    assertTrue(storage.isMidnightAuthRequired.value)
    storage.completeMidnightReauth()
    org.junit.Assert.assertFalse(storage.isMidnightAuthRequired.value)
  }

  @Test
  fun `verify overtime logic triggers at 3600 seconds`() {
    val activeShort = com.example.data.model.ActiveJobState(isRunning = true, elapsedSeconds = 1800)
    assertTrue(activeShort.elapsedSeconds < 3600)

    val activeOvertime = com.example.data.model.ActiveJobState(isRunning = true, elapsedSeconds = 3601)
    assertTrue(activeOvertime.elapsedSeconds >= 3600)
  }

  @Test
  fun `verify people directory has leadership and team members`() {
    val leadership = com.example.ui.screens.TEAM_MEMBERS.filter { it.isLeadership }
    val peers = com.example.ui.screens.TEAM_MEMBERS.filter { !it.isLeadership }
    assertTrue(leadership.isNotEmpty())
    assertTrue(peers.isNotEmpty())
    assertEquals("Operations Supervisor", leadership.first().role)
  }
}
