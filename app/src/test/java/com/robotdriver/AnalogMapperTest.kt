package com.robotdriver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalogMapperTest {

    @Test
    fun allZeroReturnsNull() {
        assertNull(pickCommand(0f, 0f, 0f))
        assertNull(AnalogMapper.pickCommand(0f, 0f, 0f))
    }

    @Test
    fun leftYPositiveAndNegative() {
        assertEquals('U', pickCommand(0f, 0.8f, 0f))
        assertEquals('D', pickCommand(0f, -0.8f, 0f))
    }

    @Test
    fun leftXPositiveAndNegative() {
        assertEquals('G', pickCommand(0.5f, 0f, 0f))
        assertEquals('O', pickCommand(-0.5f, 0f, 0f))
    }

    @Test
    fun rightYPositiveAndNegative() {
        assertEquals('E', pickCommand(0f, 0f, 0.9f))
        assertEquals('Q', pickCommand(0f, 0f, -0.9f))
    }

    @Test
    fun belowDeadzoneReturnsNull() {
        assertNull(pickCommand(0f, 0.2f, 0f))
    }

    @Test
    fun twoAxesActivePicksLarger() {
        assertEquals('E', pickCommand(0f, 0.5f, 0.9f))
    }
}
