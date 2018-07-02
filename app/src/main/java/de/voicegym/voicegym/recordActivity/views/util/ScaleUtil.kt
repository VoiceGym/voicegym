package de.voicegym.voicegym.recordActivity.views.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun getExponentialTicklist(from: Double, until: Double): ArrayList<Tick> {
    val tickList = ArrayList<Tick>()
    // get exponents of range limits
    val loExponent = BigDecimal(Math.log10(from)).round(MathContext(1, RoundingMode.DOWN)).toDouble()
    val hiExponent = BigDecimal(Math.log10(until)).round(MathContext(1, RoundingMode.DOWN)).toDouble()
    val listOfExponents = ArrayList<Double>()
    // make a list of all exponents in range
    var exponent = loExponent
    while (exponent <= hiExponent) {
        listOfExponents.add(exponent)
        exponent += 1.0
    }
    // for each exponent e check which values a*10^e (with a=1..9) are in range and add them to the ticklist
    listOfExponents.forEach {
        val base = Math.pow(10.0, it)
        if (base <= until && base >= from) {
            tickList.add(Tick(base, true))
        }
        for (i in 2..9) {
            if (i * base <= until && i * base >= from) {
                tickList.add(Tick(i * base, false))
            }
        }
    }
    return tickList
}

fun getLinearTicklist(from: Double, until: Double): ArrayList<Tick> {
    val tickList = ArrayList<Tick>()
    // initialize with highest big tick
    val highestLabel = BigDecimal(until).round(MathContext(1, RoundingMode.DOWN)).toDouble()
    val m = getOrderOfMagnitude(highestLabel, false)
    var tickValue = highestLabel + m / 10
    // small ticks above
    while (tickValue < until) {
        tickList.add(Tick(tickValue, false))
        tickValue += m / 10
    }
    // small and big ticks below
    tickValue = highestLabel
    while (tickValue > from) {
        tickList.add(Tick(tickValue, tickValue % m == 0.0))
        tickValue -= m / 10
    }
    return tickList
}

private fun getOrderOfMagnitude(ofValue: Double, above: Boolean): Double =
        Math.pow(10.0,
                BigDecimal(Math.log10(ofValue)).round(
                        MathContext(1,
                                if (above) RoundingMode.UP
                                else RoundingMode.DOWN)
                ).toDouble())


data class Tick(val value: Double, val hasLabel: Boolean)
