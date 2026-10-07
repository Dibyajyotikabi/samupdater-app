package com.samupdater.app.ui

import com.samupdater.app.ui.components.DeviceInput
import com.samupdater.app.ui.updates.RolloutCscs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputHelpersTest {

    @Test
    fun `normalizes typed model numbers`() {
        assertEquals("SM-S938B", DeviceInput.normalizeModel(" s938b "))
        assertEquals("SM-A566B", DeviceInput.normalizeModel("SM-A566B"))
        assertEquals("", DeviceInput.normalizeModel("  "))
    }

    @Test
    fun `validates model and CSC codes`() {
        assertTrue(DeviceInput.isValidModel("SM-S938B"))
        assertFalse(DeviceInput.isValidModel("SM-S9"))
        assertFalse(DeviceInput.isValidModel("SM-S938B; drop"))
        assertTrue(DeviceInput.isValidCsc("INS"))
        assertFalse(DeviceInput.isValidCsc("ins"))
        assertFalse(DeviceInput.isValidCsc("INSX"))
    }

    @Test
    fun `picks rollout regions from the catalog or the model suffix`() {
        assertEquals(listOf("EUX"), RolloutCscs.forModel("SM-S938B", listOf("EUX")))
        assertTrue("XAA" in RolloutCscs.forModel("SM-S938U1", null))
        assertTrue("TMB" in RolloutCscs.forModel("SM-S938U", emptyList()))
        assertEquals(listOf("KOO"), RolloutCscs.forModel("SM-S938N", null))
        assertTrue("CHC" in RolloutCscs.forModel("SM-S9380", null))
        assertTrue("INS" in RolloutCscs.forModel("SM-S938B", null))
    }
}
