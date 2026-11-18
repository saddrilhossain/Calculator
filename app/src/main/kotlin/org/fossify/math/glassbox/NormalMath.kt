package org.fossify.math.glassbox

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * Glassbox: the maths behind the Normal distribution screen.
 * This file has no Android code in it, so it can be tested on its own.
 */
object NormalMath {

    /** Height of the standard normal curve at z. */
    fun pdf(z: Double): Double = exp(-z * z / 2) / sqrt(2 * Math.PI)

    /** Φ(z) = P(Z < z). Accurate to about 7 decimal places — better than CAIE tables. */
    fun phi(z: Double): Double = 0.5 * erfc(-z / sqrt(2.0))

    /** Standardise: z = (x − μ) / σ */
    fun zScore(x: Double, mean: Double, sd: Double): Double = (x - mean) / sd

    // Complementary error function (Numerical Recipes "erfcc", error < 1.2e-7)
    private fun erfc(x: Double): Double {
        val z = abs(x)
        val t = 1.0 / (1.0 + 0.5 * z)
        val ans = t * exp(
            -z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
                t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 +
                t * (-0.82215223 + t * 0.17087277))))))))
        )
        return if (x >= 0) ans else 2.0 - ans
    }
}

/** The three question shapes in the MVP. */
enum class RegionType { LESS_THAN, GREATER_THAN, BETWEEN }

/**
 * One question, written the way CAIE writes it: X ~ N(μ, σ²).
 * Note: the user types the VARIANCE (σ²), exactly as it appears in the question.
 */
data class NormalQuestion(
    val mean: Double,
    val variance: Double,
    val type: RegionType,
    val a: Double,
    val b: Double = Double.NaN // only used for BETWEEN
) {
    val sd: Double get() = sqrt(variance)
    val isValid: Boolean
        get() = variance > 0 && !a.isNaN() && (type != RegionType.BETWEEN || (!b.isNaN() && b > a))

    /** The correct probability for this question. */
    fun probability(): Double = probabilityUsing(sd)

    /** Same calculation but with any σ — used to recreate the "used σ² as σ" mistake. */
    fun probabilityUsing(sigma: Double): Double {
        val za = NormalMath.zScore(a, mean, sigma)
        return when (type) {
            RegionType.LESS_THAN -> NormalMath.phi(za)
            RegionType.GREATER_THAN -> 1 - NormalMath.phi(za)
            RegionType.BETWEEN -> NormalMath.phi(NormalMath.zScore(b, mean, sigma)) - NormalMath.phi(za)
        }
    }
}
