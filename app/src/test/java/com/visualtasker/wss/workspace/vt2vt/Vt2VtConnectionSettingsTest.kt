package com.visualtasker.wss.workspace.vt2vt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Vt2VtConnectionSettingsTest {
    @Test
    fun settingsRoundTripKeepsRemoteEndpointAndReconnectIntent() {
        val settings = Vt2VtConnectionSettings(
            remoteHost = "192.168.1.42",
            port = 48123,
            remotePairingCode = "AB12CD",
            role = Vt2VtRole.CoEditor,
            liveMirrorEnabled = true,
            listenOnLaunch = true,
            autoReconnect = true,
            preferUsbBridge = false,
            connectionEnabled = true,
        )

        assertEquals(settings, Vt2VtConnectionSettings.decode(settings.encode()))
    }

    @Test
    fun defaultsDoNotReconnectWithoutExplicitConnectionIntent() {
        val settings = Vt2VtConnectionSettings.decode(null)

        assertFalse(settings.connectionEnabled)
        assertTrue(settings.autoReconnect)
        assertEquals(47272, settings.port)
    }

    @Test
    fun malformedPersistedSettingsFallBackToSafeDefaults() {
        val settings = Vt2VtConnectionSettings.decode("not-json")

        assertEquals("", settings.remoteHost)
        assertFalse(settings.connectionEnabled)
        assertEquals(Vt2VtRole.Primary, settings.role)
    }
}
