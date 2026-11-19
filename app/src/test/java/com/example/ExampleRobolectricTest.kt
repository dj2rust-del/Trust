package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `app name and support contact resources match brand`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("DJ TRUST MEGA", context.getString(R.string.app_name))
    assertEquals("+256742222056", context.getString(R.string.support_phone_raw))
  }
}
