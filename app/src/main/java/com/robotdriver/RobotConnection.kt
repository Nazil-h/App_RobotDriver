package com.robotdriver

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

private const val TAG = "RobotConnection"

sealed class RobotEvent {
    object CenterReached : RobotEvent()
    data class ArucoId(val id: Int) : RobotEvent()
    data class Status(val text: String) : RobotEvent()
}

enum class LinkState { DISCONNECTED, CONNECTING, CONNECTED }

class RobotConnection {

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandChannel = Channel<Char>(capacity = 64)

    private val _linkState = MutableStateFlow(LinkState.DISCONNECTED)
    val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    private val _events = MutableSharedFlow<RobotEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<RobotEvent> = _events.asSharedFlow()

    @Volatile private var socket: Socket? = null
    @Volatile private var lastRxMs: Long = 0L
    private var sessionJob: Job? = null

    fun connect(host: String) {
        if (sessionJob?.isActive == true) return

        // Buang perintah lama yang masih antre, supaya tidak ada F basi setelah reconnect
        while (commandChannel.tryReceive().isSuccess) { }

        sessionJob = ioScope.launch {
            try {
                _linkState.value = LinkState.CONNECTING
                val s = Socket().apply { tcpNoDelay = true }
                s.connect(InetSocketAddress(host, Protocol.CONTROL_PORT), 3000)
                socket = s
                lastRxMs = System.currentTimeMillis()
                _linkState.value = LinkState.CONNECTED

                send(Protocol.CMD_STOP)

                coroutineScope {
                    launch { try { readLoop(BufferedReader(InputStreamReader(s.getInputStream()))) } finally { closeSocket() } }
                    launch { try { writeLoop(s.getOutputStream()) } finally { closeSocket() } }
                    launch { heartbeatLoop() }
                    launch { try { watchdogLoop() } finally { closeSocket() } }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sesi berakhir: ${e.message}")
            } finally {
                closeSocket()
                _linkState.value = LinkState.DISCONNECTED
            }
        }
    }

    fun disconnect() {
        ioScope.launch {
            try {
                socket?.getOutputStream()?.apply {
                    write("S\n".toByteArray())
                    flush()
                }
            } catch (_: Exception) { }
            sessionJob?.cancel()
            closeSocket()
        }
    }

    fun send(cmd: Char) {
        commandChannel.trySend(cmd)
    }

    private fun closeSocket() {
        try { socket?.close() } catch (_: Exception) { }
        socket = null
    }

    private suspend fun readLoop(reader: BufferedReader) {
        while (true) {
            val line = withContext(Dispatchers.IO) { reader.readLine() }
                ?: throw IOException("Koneksi ditutup hub")
            lastRxMs = System.currentTimeMillis()
            parseLine(line.trim())
        }
    }

    private suspend fun writeLoop(out: OutputStream) {
        for (cmd in commandChannel) {
            withContext(Dispatchers.IO) {
                out.write("$cmd\n".toByteArray())
                out.flush()
            }
        }
    }

    private suspend fun heartbeatLoop() {
        while (true) {
            send(Protocol.CMD_HEARTBEAT)
            delay(Protocol.HEARTBEAT_INTERVAL_MS)
        }
    }

    private suspend fun watchdogLoop() {
        while (true) {
            delay(300)
            if (System.currentTimeMillis() - lastRxMs > Protocol.HUB_SILENCE_TIMEOUT_MS) {
                closeSocket()
                throw IOException("Hub tidak merespons")
            }
        }
    }

    private fun parseLine(line: String) {
        when {
            line == "C" -> _events.tryEmit(RobotEvent.CenterReached)
            line.startsWith("I:") -> line.removePrefix("I:").toIntOrNull()
                ?.let { _events.tryEmit(RobotEvent.ArucoId(it)) }
            line.startsWith("T:") -> {
                val text = line.removePrefix("T:")
                if (text != "OK") _events.tryEmit(RobotEvent.Status(text))
            }
        }
    }
}