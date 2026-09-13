package org.fundamentalos.weather.ui.components

import kotlin.math.sqrt

/**
 * Piecewise Cubic Hermite Interpolating Polynomial (PCHIP) with Fritsch-Carlson tangent
 * estimation. Guarantees monotonicity and C¹ continuity — no overshoot between knots.
 *
 * xs must be strictly increasing; xs and ys must have the same length (≥ 2).
 */
class Pchip(private val xs: DoubleArray, private val ys: DoubleArray) {
    init {
        require(xs.size == ys.size && xs.size >= 2) { "Need at least 2 knots with matching x/y" }
    }

    private val n = xs.size
    private val tangents: DoubleArray = computeTangents()

    private fun computeTangents(): DoubleArray {
        val h = DoubleArray(n - 1) { xs[it + 1] - xs[it] }
        val delta = DoubleArray(n - 1) { (ys[it + 1] - ys[it]) / h[it] }
        val m = DoubleArray(n)

        m[0] = delta[0]
        m[n - 1] = delta[n - 2]
        for (i in 1 until n - 1) {
            m[i] = if (delta[i - 1] * delta[i] <= 0.0) {
                0.0
            } else {
                val w1 = 2.0 * h[i] + h[i - 1]
                val w2 = h[i] + 2.0 * h[i - 1]
                (w1 + w2) / (w1 / delta[i - 1] + w2 / delta[i])
            }
        }

        // Fritsch-Carlson monotonicity fix
        for (i in 0 until n - 1) {
            if (delta[i] == 0.0) {
                m[i] = 0.0; m[i + 1] = 0.0
            } else {
                val alpha = m[i] / delta[i]
                val beta = m[i + 1] / delta[i]
                val r = alpha * alpha + beta * beta
                if (r > 9.0) {
                    val tau = 3.0 / sqrt(r)
                    m[i] = tau * alpha * delta[i]
                    m[i + 1] = tau * beta * delta[i]
                }
            }
        }

        return m
    }

    fun evaluate(x: Double): Double {
        val i = xs.indexOfLast { it <= x }.coerceIn(0, n - 2)
        val h = xs[i + 1] - xs[i]
        val t = (x - xs[i]) / h
        val t2 = t * t
        val t3 = t2 * t
        return ys[i] * (2 * t3 - 3 * t2 + 1) +
            ys[i + 1] * (-2 * t3 + 3 * t2) +
            tangents[i] * h * t * (t - 1) * (t - 1) +
            tangents[i + 1] * h * t2 * (t - 1)
    }
}
