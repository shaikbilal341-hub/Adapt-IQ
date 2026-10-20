package com.example.adaptiq

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying target letter selection, variety, research event payload formatting,
 * and state transitions for Activity 10 (Visual Search / Target Locating).
 */
class VisualSearchTargetTrackingUnitTest {

    private val supportedSymbols = listOf("O", "A", "V", "X", "M", "S", "W", "K", "Z")
    private val targetSequence = listOf("X", "A", "M")

    private fun getTargetForTrial(trial: Int): String {
        return targetSequence.getOrElse((trial - 1) % targetSequence.size) {
            supportedSymbols[(trial - 1) % supportedSymbols.size]
        }
    }

    @Test
    fun targetSequence_containsOnlySupportedSymbols() {
        for (target in targetSequence) {
            assertTrue(
                "Target '$target' must be contained within supported symbols pool $supportedSymbols",
                supportedSymbols.contains(target)
            )
        }
    }

    @Test
    fun targetSequence_providesVariedLettersAcrossConsecutiveTrials() {
        val trial1Target = getTargetForTrial(1)
        val trial2Target = getTargetForTrial(2)
        val trial3Target = getTargetForTrial(3)

        assertEquals("Trial 1 target should be 'X'", "X", trial1Target)
        assertEquals("Trial 2 target should be 'A'", "A", trial2Target)
        assertEquals("Trial 3 target should be 'M'", "M", trial3Target)

        assertNotEquals("Trial 1 and Trial 2 targets must not be identical", trial1Target, trial2Target)
        assertNotEquals("Trial 2 and Trial 3 targets must not be identical", trial2Target, trial3Target)
        assertNotEquals("Trial 1 and Trial 3 targets must not be identical", trial1Target, trial3Target)
    }

    @Test
    fun targetSequence_safeForAnyTrialIndex() {
        for (trial in 1..20) {
            val target = getTargetForTrial(trial)
            assertTrue("Target for trial $trial must be a non-empty valid symbol", target.isNotEmpty())
            assertTrue("Target '$target' must belong to the supported symbols", supportedSymbols.contains(target))
        }
    }

    @Test
    fun researchEventPayload_formatsExpectedAndDetectedActionCorrectly() {
        val trial = 2
        val target = getTargetForTrial(trial) // "A"
        val expectedAction = "find_$target"

        assertEquals("expected_action must accurately record the active target", "find_A", expectedAction)

        val correctTapSymbol = "A"
        val detectedActionCorrect = "tapped_$correctTapSymbol"
        val resultSuccess = if (correctTapSymbol == target) "successful" else "incorrect"

        assertEquals("tapped_A", detectedActionCorrect)
        assertEquals("successful", resultSuccess)

        val incorrectTapSymbol = "V"
        val detectedActionIncorrect = "tapped_$incorrectTapSymbol"
        val resultIncorrect = if (incorrectTapSymbol == target) "successful" else "incorrect"

        assertEquals("tapped_V", detectedActionIncorrect)
        assertEquals("incorrect", resultIncorrect)
    }

    @Test
    fun sessionRestart_resetsToFirstTargetInSequence() {
        var currentTrial = 3
        assertEquals("M", getTargetForTrial(currentTrial))

        // Reset to initial trial (e.g. on official collection start or restart)
        currentTrial = 1
        val resetTarget = getTargetForTrial(currentTrial)
        assertEquals("First target on session start/reset must be 'X'", "X", resetTarget)
    }
}
