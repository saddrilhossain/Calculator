package org.fossify.math.activities

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.NavigationIcon
import org.fossify.math.R
import org.fossify.math.databinding.ActivityGlassboxNormalBinding
import org.fossify.math.glassbox.Diagnosis
import org.fossify.math.glassbox.NormalQuestion
import org.fossify.math.glassbox.RegionType

/**
 * Glassbox — a transparent mentor for CAIE Normal distribution questions.
 * The student does the thinking; this screen only makes their thinking visible.
 */
class GlassboxNormalActivity : SimpleActivity() {

    private val binding by viewBinding(ActivityGlassboxNormalBinding::inflate)
    private var question: NormalQuestion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupEdgeToEdge(padBottomSystem = listOf(binding.glassboxScrollview))
        setupMaterialScrollListener(binding.glassboxScrollview, binding.glassboxAppbar)

        // Show the "b" box only when the student picks P(a < X < b)
        binding.regionGroup.setOnCheckedChangeListener { _, checkedId ->
            binding.bHolder.visibility = if (checkedId == R.id.region_between) View.VISIBLE else View.GONE
        }

        binding.btnShowRegion.setOnClickListener { showRegion() }
        binding.btnCheckZ.setOnClickListener { checkZ() }
        binding.btnCheckAnswer.setOnClickListener { checkAnswer() }
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.glassboxAppbar, NavigationIcon.Arrow)
        applyColors()
    }

    // ---------- Step 1: draw the region the student described ----------

    private fun showRegion() {
        hideKeyboard()
        val type = when (binding.regionGroup.checkedRadioButtonId) {
            R.id.region_greater -> RegionType.GREATER_THAN
            R.id.region_between -> RegionType.BETWEEN
            else -> RegionType.LESS_THAN
        }
        val q = NormalQuestion(
            mean = binding.inputMean.number() ?: return say("⚠️ Enter the mean μ."),
            variance = binding.inputVariance.number() ?: return say("⚠️ Enter the variance σ²."),
            type = type,
            a = binding.inputA.number() ?: return say("⚠️ Enter the boundary a."),
            b = binding.inputB.number() ?: Double.NaN
        )
        if (!q.isValid) {
            return say("⚠️ Check your numbers: the variance must be positive, and b must be bigger than a.")
        }
        question = q
        binding.normalCurve.showQuestion(q)
        say("👀 Is this the area your question is asking for? If not, change the inequality before you calculate anything.")
    }

    // ---------- Step 2: where does the student's z land? ----------

    private fun checkZ() {
        hideKeyboard()
        val q = question ?: return say("⚠️ Tap 'Show region' first.")
        val z = binding.inputZ.number() ?: return say("⚠️ Enter your z-value.")
        binding.normalCurve.showStudentZ(z)
        say(Diagnosis.checkZ(q, z).message)
    }

    // ---------- Step 3: mentor feedback on the final answer ----------

    private fun checkAnswer() {
        hideKeyboard()
        val q = question ?: return say("⚠️ Tap 'Show region' first.")
        val p = binding.inputAnswer.number() ?: return say("⚠️ Enter your final probability.")
        say(Diagnosis.checkAnswer(q, p).message)
    }

    // ---------- helpers ----------

    private fun say(message: String) {
        binding.feedback.text = message
    }

    private fun EditText.number(): Double? = text.toString().trim().toDoubleOrNull()

    /** Paint every text, input and button in the user's chosen Fossify colours. */
    private fun applyColors() {
        val text = getProperTextColor()
        val primary = getProperPrimaryColor()
        binding.normalCurve.setColors(text, primary)
        tint(binding.glassboxHolder, text, primary)
    }

    private fun tint(view: View, text: Int, primary: Int) {
        when (view) {
            // RadioButton is a kind of Button, so it must be checked first
            is CompoundButton -> {
                view.setTextColor(text)
                view.buttonTintList = ColorStateList.valueOf(primary)
            }
            is Button -> {
                view.backgroundTintList = ColorStateList.valueOf(primary)
                view.setTextColor(primary.getContrastColor())
            }
            is EditText -> {
                view.setTextColor(text)
                view.setHintTextColor(text and 0x00FFFFFF or 0x80000000.toInt())
                view.backgroundTintList = ColorStateList.valueOf(primary)
            }
            is TextView -> view.setTextColor(text)
            is ViewGroup -> for (i in 0 until view.childCount) tint(view.getChildAt(i), text, primary)
        }
    }
}
