package com.robotdriver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class RobotViewModel : ViewModel() {

    val connection = RobotConnection()
    val video = VideoClient()

    var host by mutableStateOf("192.168.43.1")

    var lastArucoId by mutableStateOf<Int?>(null)
        private set
    var lastStatus by mutableStateOf("")
        private set
    var centerReached by mutableStateOf(value = false)
        private set

    init {
        viewModelScope.launch {
            connection.events.collect { e ->
                when (e) {
                    is RobotEvent.ArucoId -> lastArucoId = e.id
                    RobotEvent.CenterReached -> centerReached = true
                    is RobotEvent.Status -> lastStatus = e.text
                }
            }
        }
    }

    fun connect() {
        lastArucoId = null
        centerReached = false
        connection.connect(host)
        video.start(host)
    }

    fun disconnect() {
        connection.disconnect()
        video.stop()
        lastArucoId = null
        centerReached = false
    }

    override fun onCleared() {
        disconnect()
    }
}
