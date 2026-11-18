package org.fossify.math.glassbox

import kotlin.math.abs
import kotlin.math.max

/**
 * Glassbox's "transparent mentor".
 * It recreates the common wrong answers and matches the student's answer against them.
 * It NEVER reveals the correct value — it only points at the concept to re-check.
 */
object Diagnosis {

    enum class Verdict { CORRECT, KNOWN_MISTAKE, UNKNOWN, INVALID }

    data class Feedback(val verdict: Verdict, val message: String)

    /** Answers within this distance count as "the same" (allows for table rounding, 3 s.f.). */
    private fun matches(student: Double, target: Double): Boolean =
        abs(student - target) <= max(0.0015, 0.005 * abs(target))

    // ---------------- z-score check ----------------

    fun checkZ(q: NormalQuestion, studentZ: Double, forB: Boolean = false): Feedback {
        val x = if (forB) q.b else q.a
        val correct = NormalMath.zScore(x, q.mean, q.sd)

        if (matches(studentZ, correct)) {
            return Feedback(Verdict.CORRECT, "✅ Your z-value is correct. Its marker sits exactly on your boundary.")
        }

        val mistakes = listOf(
            -correct to "💡 Check the sign. Is your value of x above or below the mean? Your z should be " +
                "positive if x is above μ and negative if below.",
            NormalMath.zScore(x, q.mean, q.variance) to "💡 Look at the question again: X ~ N(μ, σ²). " +
                "The second number is the VARIANCE. Did you divide by σ² instead of σ?",
            x / q.sd to "💡 Did you forget to subtract the mean? z = (x − μ) / σ.",
            (q.mean - x) / q.variance to "💡 Two things to check: the order of (x − μ) and whether you used σ² instead of σ."
        )
        return mistakes.firstOrNull { matches(studentZ, it.first) && !matches(it.first, correct) }
            ?.let { Feedback(Verdict.KNOWN_MISTAKE, it.second) }
            ?: Feedback(
                Verdict.UNKNOWN,
                "❌ That z-value doesn't match your boundary. Look at the orange marker — " +
                    "is it where your x-value should be?"
            )
    }

    // ---------------- final answer check ----------------

    fun checkAnswer(q: NormalQuestion, studentP: Double): Feedback {
        if (studentP < 0 || studentP > 1) {
            return Feedback(
                Verdict.INVALID,
                "⚠️ A probability must be between 0 and 1. Did you type your z-value by mistake?"
            )
        }

        val correct = q.probability()
        if (matches(studentP, correct)) {
            return Feedback(Verdict.CORRECT, "✅ Correct! Your answer matches the shaded area.")
        }

        val za = NormalMath.zScore(q.a, q.mean, q.sd)
        val pa = NormalMath.phi(za)

        val mistakes = mutableListOf<Pair<Double, String>>()

        when (q.type) {
            RegionType.LESS_THAN -> {
                mistakes += 1 - correct to "💡 Your answer is the area on the OTHER side of the boundary. " +
                    "Look at the shading: you want the left side. Remember Φ(z) gives the area to the LEFT of z."
            }

            RegionType.GREATER_THAN -> {
                mistakes += pa to "💡 Φ(z) gives the area to the LEFT of z, but the shading is on the RIGHT. " +
                    "What do you need to do to a left-hand area to get the right-hand one?"
            }

            RegionType.BETWEEN -> {
                val zb = NormalMath.zScore(q.b, q.mean, q.sd)
                val pb = NormalMath.phi(zb)
                mistakes += 1 - correct to "💡 You found the area OUTSIDE the shaded region. " +
                    "You want the part between the two boundaries."
                mistakes += pb to "💡 You found everything to the left of the upper boundary. " +
                    "What do you need to remove from that area?"
                if (za < 0) {
                    mistakes += pb - NormalMath.phi(-za) to "💡 Check how you found Φ of your NEGATIVE z. " +
                        "Tables only give positive z: Φ(−z) = 1 − Φ(z)."
                }
                mistakes += pb + pa to "💡 Areas between two boundaries are found by SUBTRACTING, not adding."
            }
        }

        // Applies to every question type: used the variance as if it were σ
        mistakes += q.probabilityUsing(q.variance) to "💡 X ~ N(μ, σ²): the second number is the VARIANCE. " +
            "Did you use it as σ without square-rooting?"

        return mistakes.firstOrNull { matches(studentP, it.first) && !matches(it.first, correct) }
            ?.let { Feedback(Verdict.KNOWN_MISTAKE, it.second) }
            ?: Feedback(
                Verdict.UNKNOWN,
                "❌ Not quite. Check each step: z-value → Φ(z) from the table → which side is shaded?"
            )
    }
}
