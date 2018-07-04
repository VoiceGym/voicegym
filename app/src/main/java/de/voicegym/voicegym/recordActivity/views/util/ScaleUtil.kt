package de.voicegym.voicegym.recordActivity.views.util

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

fun getExponentialTicklist(from: Double, until: Double): List<Tick> {
    // get exponents of range limits
    val loExponent = BigDecimal(Math.log10(from)).round(MathContext(1, RoundingMode.DOWN)).toInt()
    val hiExponent = BigDecimal(Math.log10(until)).round(MathContext(1, RoundingMode.DOWN)).toInt()
    val exponents = loExponent..hiExponent

    // for each exponent e check which values a*10^e (with a=1..9) are in range and add them to the ticklist
    return exponents.flatMap { exponent ->
        val base = Math.pow(10.0, exponent.toDouble())
        (1..9).filter { it * base in from..until }
            .map {
                Tick(it * base, it == 1)
            }
    }

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
