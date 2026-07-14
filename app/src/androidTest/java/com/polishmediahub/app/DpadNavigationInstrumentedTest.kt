package com.polishmediahub.app

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DpadNavigationInstrumentedTest {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun dpadKeysAreDelivered() {
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_DPAD_DOWN))
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_DPAD_CENTER))
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_DPAD_UP))
        assertTrue(device.pressKeyCode(KeyEvent.KEYCODE_DPAD_LEFT))
    }
}
