package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.CrosshairViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CoreBugFixTest {

    private lateinit var app: Application
    private lateinit var viewModel: CrosshairViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("crosshair_pro_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        viewModel = CrosshairViewModel(app)
    }

    @Test
    fun aimTrainerAccuracyReaches100PercentWhenAllShotsHit() {
        viewModel.startAimTrainer()
        assertEquals(0, viewModel.gameState.value.totalTargets)

        viewModel.onTargetHit(isHeadshot = false)
        viewModel.onTargetHit(isHeadshot = true)
        viewModel.onTargetHit(isHeadshot = false)

        val stats = viewModel.gameState.value
        assertEquals(3, stats.targetsHit)
        assertEquals(3, stats.totalTargets)
        val accuracy = (stats.targetsHit * 100) / stats.totalTargets
        assertEquals(100, accuracy)
    }

    @Test
    fun parseAndApplyHexStringForcesOpaqueAlphaForEightCharInput() {
        val applied = viewModel.parseAndApplyHexString("#0000E5FF")
        assertTrue(applied)
        assertEquals(0xFF00E5FFL, viewModel.activeConfig.value.primaryColorHex)
    }

    @Test
    fun encodeAndImportCrosshairCodePreservesSemicolonsInNameAndNormalizesColor() {
        val original = CrosshairConfig(
            id = "test_semi",
            name = "CQB; Rush Pro",
            style = CrosshairStyle.CYBER_HEX,
            sizeDp = 38f,
            thicknessDp = 3f,
            gapDp = 6f,
            centerDot = true,
            centerDotSizeDp = 4f,
            rotationDeg = 45f,
            opacity = 0.85f,
            primaryColorHex = 0xFF00E5FFL,
            outlineEnabled = true,
            tStyle = false,
            isCustom = true
        )

        val code = viewModel.encodeCrosshairCode(original)
        val imported = viewModel.importCrosshairCode(code)

        assertTrue(imported)
        assertEquals("CQB; Rush Pro", viewModel.activeConfig.value.name)
        assertEquals(CrosshairStyle.CYBER_HEX, viewModel.activeConfig.value.style)
        assertEquals(0xFF00E5FFL, viewModel.activeConfig.value.primaryColorHex)
    }

    @Test
    fun deletingActiveCustomPresetDetachesDeletedIdWhileKeepingVisualSettings() {
        val saved = viewModel.saveConfigAsPreset(name = "Delete Me")
        assertEquals(saved.id, viewModel.activeConfig.value.id)

        viewModel.confirmDeletePreset(saved)

        assertNotEquals(saved.id, viewModel.activeConfig.value.id)
        assertEquals(saved.style, viewModel.activeConfig.value.style)
        assertEquals(saved.primaryColorHex, viewModel.activeConfig.value.primaryColorHex)
    }
}
