package com.c242_ps246.mentalq

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationContextTest {
    @Test
    fun applicationUsesExpectedPackageAndClass() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.c242_ps246.mentalq", context.packageName)
        assertTrue(context.applicationContext is MentalQApp)
    }
}
