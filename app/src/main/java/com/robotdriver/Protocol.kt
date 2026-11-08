package com.robotdriver

object Protocol {
    const val CONTROL_PORT = 5000
    const val VIDEO_PORT = 5001

    const val HEARTBEAT_INTERVAL_MS = 150L
    const val HUB_SILENCE_TIMEOUT_MS = 1500L
    const val MAX_FRAME_BYTES = 500_000

    const val CMD_FORWARD = 'F'
    const val CMD_BACK = 'B'
    const val CMD_LEFT = 'L'
    const val CMD_RIGHT = 'R'
    const val CMD_STOP = 'S'
    const val CMD_HEARTBEAT = 'H'
    const val CMD_MODE_AUTO = 'A'
    const val CMD_MODE_MANUAL = 'M'

    // Lengan (bahu), gripper, dan siku
    const val CMD_ARM_UP = 'U'
    const val CMD_ARM_DOWN = 'D'
    const val CMD_GRIP_OPEN = 'O'
    const val CMD_GRIP_CLOSE = 'G'
    const val CMD_ELBOW_UP = 'E'
    const val CMD_ELBOW_DOWN = 'Q'

    const val ARM_REPEAT_MS = 100L
    const val ANALOG_DEADZONE = 0.3f
}