package com.robotdriver

import kotlin.math.abs

object AnalogMapper {
    const val DEADZONE = 0.3f

    fun pickCommand(leftX: Float, leftY: Float, rightY: Float): Char? =
        com.robotdriver.pickCommand(leftX, leftY, rightY)
}

fun pickCommand(leftX: Float, leftY: Float, rightY: Float): Char? {
    val axes = listOf(
        Triple(leftY, 'U', 'D'),
        Triple(leftX, 'G', 'O'),
        Triple(rightY, 'E', 'Q'),
    )

    var bestMag = 0f
    var best: Char? = null
    for ((value, positive, negative) in axes) {
        val mag = abs(value)
        if ((mag < AnalogMapper.DEADZONE) || (mag <= bestMag)) continue
        bestMag = mag
        best = if (value > 0f) positive else negative
    }
    return best
}
