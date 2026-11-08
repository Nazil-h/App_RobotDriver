package com.robotdriver

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class VideoClient {

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _frame = MutableStateFlow<Bitmap?>(null)
    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var job: Job? = null
    @Volatile private var sock: Socket? = null

    fun start(host: String) {
        if (job?.isActive == true) return

        job = ioScope.launch {
            while (true) {
                try {
                    val s = Socket().also { sock = it }
                    s.connect(InetSocketAddress(host, Protocol.VIDEO_PORT), 3000)
                    val input = DataInputStream(BufferedInputStream(s.getInputStream()))
                    _isStreaming.value = true

                    while (true) {
                        val len = input.readInt()
                        if (len <= 0 || len > Protocol.MAX_FRAME_BYTES) {
                            throw IOException("Panjang frame tidak valid: $len")
                        }
                        val bytes = ByteArray(len)
                        input.readFully(bytes)
                        BitmapFactory.decodeByteArray(bytes, 0, len)?.let { _frame.value = it }
                    }
                } catch (e: Exception) {
                    _frame.value = null
                    _isStreaming.value = false
                }
                closeSock()
                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        closeSock()
        _frame.value = null
        _isStreaming.value = false
    }

    private fun closeSock() {
        try { sock?.close() } catch (_: Exception) { }
        sock = null
        _isStreaming.value = false
    }
}
