package com.example.portableevm.arduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ArduinoConnectionState {
    object Disconnected : ArduinoConnectionState()
    object Connecting : ArduinoConnectionState()
    data class Connected(val device: UsbDevice) : ArduinoConnectionState()
}

class ArduinoManager(
    private val context: Context,
    private val usbManager: UsbManager,
) {
    companion object {
        private const val TAG = "ArduinoManager"
        private const val ACTION_USB_PERMISSION = "com.example.portableevm.USB_PERMISSION"
    }

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow<ArduinoConnectionState>(ArduinoConnectionState.Disconnected)
    val connectionState: StateFlow<ArduinoConnectionState> = _connectionState

    private val _buttonEvents = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val buttonEvents: SharedFlow<Int> = _buttonEvents

    private var connection: UsbDeviceConnection? = null
    private var port: UsbSerialPort? = null
    private var readJob: Job? = null

    private val permissionIntent: PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(ACTION_USB_PERMISSION),
        PendingIntent.FLAG_IMMUTABLE
    )

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                        connectToDevice(device)
                    } else {
                        Log.w(TAG, "USB permission denied for device $device")
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    device?.let { tryOpenDevice(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null && device == port?.device) {
                        disconnect()
                    }
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        // On Android 13+ we must specify whether this receiver is exported or not.
        // Use NOT_EXPORTED because we only care about system and in-app broadcasts.
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        probeExistingDevices()
    }

    fun stop() {
        disconnect()
        runCatching { context.unregisterReceiver(receiver) }
        scope.cancel()
    }

    private fun probeExistingDevices() {
        val devices = usbManager.deviceList.values
        // Try to find a known Arduino/USB-serial vendor first, otherwise just pick the first device.
        val target = devices.firstOrNull { isArduinoLikeDevice(it) } ?: devices.firstOrNull()
        if (target != null) {
            tryOpenDevice(target)
        } else {
            Log.d(TAG, "No USB device found for Arduino connection")
        }
    }

    private fun isArduinoLikeDevice(device: UsbDevice): Boolean {
        // Common vendor IDs for Arduino and typical USB-serial bridges (Uno, CH340, CP210x, FTDI).
        return when (device.vendorId) {
            0x2341, // Arduino
            0x2A03, // Arduino (alternate)
            0x1A86, // CH340
            0x10C4, // CP210x
            0x0403  // FTDI
            -> true
            else -> false
        }
    }

    private fun tryOpenDevice(device: UsbDevice) {
        if (!usbManager.hasPermission(device)) {
            usbManager.requestPermission(device, permissionIntent)
        } else {
            connectToDevice(device)
        }
    }

    private fun connectToDevice(device: UsbDevice) {
        _connectionState.value = ArduinoConnectionState.Connecting
        val prober = UsbSerialProber.getDefaultProber()
        val driver = prober.probeDevice(device)
        if (driver == null || driver.ports.isEmpty()) {
            Log.e(TAG, "No serial driver for device $device")
            _connectionState.value = ArduinoConnectionState.Disconnected
            return
        }
        val port = driver.ports.first()
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            Log.e(TAG, "Cannot open USB device connection")
            _connectionState.value = ArduinoConnectionState.Disconnected
            return
        }

        this.connection = connection
        this.port = port

        try {
            port.open(connection)
            port.setParameters(
                9600,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            _connectionState.value = ArduinoConnectionState.Connected(device)
            startReading()
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring serial port", e)
            disconnect()
        }
    }

    private fun startReading() {
        readJob?.cancel()
        val buffer = ByteArray(16)
        readJob = scope.launch {
            while (true) {
                try {
                    val len = port?.read(buffer, 1000) ?: break
                    if (len > 0) {
                        for (i in 0 until len) {
                            val ch = buffer[i].toInt().toChar()
                            val buttonNumber = when (ch) {
                                '1' -> 1
                                '2' -> 2
                                '3' -> 3
                                '4' -> 4
                                else -> null
                            }
                            if (buttonNumber != null) {
                                _buttonEvents.emit(buttonNumber)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from serial port", e)
                    break
                }
            }
            disconnect()
        }
    }

    private fun disconnect() {
        readJob?.cancel()
        readJob = null
        try {
            port?.close()
        } catch (_: Exception) {
        }
        connection?.close()
        port = null
        connection = null
        _connectionState.value = ArduinoConnectionState.Disconnected
    }
}
