package com.visualtasker.wss.workspace.vt2vt

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.provider.Settings

const val VT2VT_ADB_BRIDGE_HOST = "127.0.0.1"
const val VT2VT_ADB_BRIDGE_PORT = 47272

data class Vt2VtUsbAdbBridgeStatus(
    val usbConnected: Boolean,
    val adbEnabled: Boolean,
    val usbConfigured: Boolean,
    val connectedPeripheralCount: Int,
    val recommendedEndpoint: Vt2VtLanEndpoint = Vt2VtLanEndpoint(VT2VT_ADB_BRIDGE_HOST, VT2VT_ADB_BRIDGE_PORT)
) {
    val bridgeReady: Boolean get() = usbConnected && adbEnabled
    val transport: Vt2VtTransport get() = if (bridgeReady) Vt2VtTransport.UsbAdbBridge else Vt2VtTransport.LanTcp
    val summary: String
        get() = when {
            bridgeReady -> "USB/ADB erkannt: localhost:${recommendedEndpoint.port}"
            usbConnected -> "USB erkannt, ADB nicht aktiv"
            connectedPeripheralCount > 0 -> "USB Host: $connectedPeripheralCount Gerät(e)"
            else -> "Kein USB/ADB erkannt"
        }
}

object Vt2VtUsbAdbBridge {
    fun detect(context: Context): Vt2VtUsbAdbBridgeStatus {
        val usbState = runCatching {
            context.registerReceiver(null, IntentFilter(USB_STATE_ACTION))
        }.getOrNull()
        val usbConnected = usbState?.getBooleanExtra("connected", false) == true
        val usbConfigured = usbState?.getBooleanExtra("configured", false) == true
        val adbFromBroadcast = usbState?.getBooleanExtra("adb", false) == true
        val adbFromSettings = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        }.getOrDefault(false)
        val peripheralCount = runCatching {
            (context.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.size
        }.getOrDefault(0)
        return Vt2VtUsbAdbBridgeStatus(
            usbConnected = usbConnected || peripheralCount > 0,
            adbEnabled = adbFromBroadcast || adbFromSettings,
            usbConfigured = usbConfigured,
            connectedPeripheralCount = peripheralCount
        )
    }
}

private const val USB_STATE_ACTION = "android.hardware.usb.action.USB_STATE"
