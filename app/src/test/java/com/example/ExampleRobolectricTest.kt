package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.data.CineRepository
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.ScrapeUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CinePremium", appName)
  }

  @Test
  fun `test cine repository vip status default`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CineRepository(context)
    val defaultVip = repository.isUserVipFlow.first()
    assertFalse(defaultVip)
  }

  @Test
  fun `test cine repository toggle vip status`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CineRepository(context)
    
    repository.setVipStatus(true)
    var isVip = repository.isUserVipFlow.first()
    assertTrue(isVip)

    repository.setVipStatus(false)
    isVip = repository.isUserVipFlow.first()
    assertFalse(isVip)
  }

  @Test
  fun `test movie viewmodel basic initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CineRepository(context)
    val viewModel = MovieViewModel(repository)

    assertEquals(ScrapeUiState.Idle, viewModel.scrapeState.value)
    assertFalse(viewModel.isUserVip.value)
  }

  @Test
  fun `test whatsapp subscription intent content`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = CineRepository(context)
    val viewModel = MovieViewModel(repository)

    viewModel.startWhatsAppSubscriptionIntent(context)

    // Intercept started activities
    val shadowApp = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
    val startedIntent = shadowApp.nextStartedActivity
    
    assertNotNull(startedIntent)
    assertEquals(Intent.ACTION_VIEW, startedIntent.action)
    assertTrue(startedIntent.data.toString().contains("api.whatsapp.com"))
    assertTrue((startedIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    
    // Decode URL to parse special encoded characters
    val decodedUri = java.net.URLDecoder.decode(startedIntent.data.toString(), "UTF-8")
    assertTrue(decodedUri.contains("text="))
    assertTrue(decodedUri.contains("Olá! Gostaria de assinar o plano VIP do app. Meu ID é:"))
  }
}
