package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.room.AppDatabase
import com.example.data.room.PresetEntity
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.CrosshairViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomPresetViewModelTest {

    private lateinit var app: Application
    private lateinit var viewModel: CrosshairViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = CrosshairViewModel(app)
        clearAllSavedPresets()
        viewModel.resetToFactoryDefaults()
        viewModel.clearPresetFeedbackMessage()
    }

    @After
    fun tearDown() {
        clearAllSavedPresets()
        viewModel.closeSaveDialog()
        viewModel.closeRenameDialog()
        viewModel.closeDeleteConfirmDialog()
        viewModel.clearPresetFeedbackMessage()
    }

    private fun clearAllSavedPresets() {
        viewModel.savedPresets.value.toList().forEach { preset ->
            viewModel.deletePreset(preset.id)
        }
    }

    @Test
    fun `saveCurrentAsPreset captures size color opacity rotation and custom name`() {
        viewModel.setReticleSizeDp(54f)
        viewModel.setPrimaryColorHex(0xFF00E5FF)
        viewModel.setReticleOpacity(0.75f)
        viewModel.setReticleRotationDeg(45f)

        val saved = viewModel.saveConfigAsPreset(
            name = "  Sniper 45°  ",
            category = CrosshairConfig.CATEGORY_SNIPER
        )

        assertTrue(saved.isCustom)
        assertEquals("Sniper 45°", saved.name)
        assertEquals(CrosshairConfig.CATEGORY_SNIPER, saved.category)
        assertEquals(54f, saved.sizeDp, 0.01f)
        assertEquals(0xFF00E5FF, saved.primaryColorHex)
        assertEquals(0.75f, saved.opacity, 0.001f)
        assertEquals(45f, saved.rotationDeg, 0.01f)

        val active = viewModel.activeConfig.value
        assertEquals(saved.id, active.id)
        assertEquals("Sniper 45°", active.name)
        assertTrue(viewModel.savedPresets.value.any { it.id == saved.id })
        assertNotNull(viewModel.presetFeedbackMessage.value)
    }

    @Test
    fun `quickSwitchPreset restores full configuration across multiple saved presets`() {
        viewModel.updateConfig { it.copy(style = CrosshairStyle.MIL_DOT_SNIPER) }
        viewModel.setReticleSizeDp(62f)
        viewModel.setPrimaryColorHex(0xFF00E5FF)
        viewModel.setReticleOpacity(0.65f)
        viewModel.setReticleRotationDeg(45f)
        val sniperPreset = viewModel.saveConfigAsPreset(name = "Sniper Cyan")

        viewModel.updateConfig { it.copy(style = CrosshairStyle.SHOTGUN_BOX) }
        viewModel.setReticleSizeDp(28f)
        viewModel.setPrimaryColorHex(0xFFFF1744)
        viewModel.setReticleOpacity(0.95f)
        viewModel.setReticleRotationDeg(135f)
        val cqbPreset = viewModel.saveConfigAsPreset(name = "CQB Crimson")

        assertNotEquals(sniperPreset.id, cqbPreset.id)
        assertEquals(2, viewModel.savedPresets.value.size)

        // Switch back to Sniper preset
        viewModel.quickSwitchPreset(sniperPreset)
        with(viewModel.activeConfig.value) {
            assertEquals(sniperPreset.id, id)
            assertEquals("Sniper Cyan", name)
            assertEquals(CrosshairStyle.MIL_DOT_SNIPER, style)
            assertEquals(62f, sizeDp, 0.01f)
            assertEquals(0xFF00E5FF, primaryColorHex)
            assertEquals(0.65f, opacity, 0.001f)
            assertEquals(45f, rotationDeg, 0.01f)
        }

        // Switch to CQB preset
        viewModel.quickSwitchPreset(cqbPreset)
        with(viewModel.activeConfig.value) {
            assertEquals(cqbPreset.id, id)
            assertEquals("CQB Crimson", name)
            assertEquals(CrosshairStyle.SHOTGUN_BOX, style)
            assertEquals(28f, sizeDp, 0.01f)
            assertEquals(0xFFFF1744, primaryColorHex)
            assertEquals(0.95f, opacity, 0.001f)
            assertEquals(135f, rotationDeg, 0.01f)
        }

        // Cycle to next custom preset wraps cleanly
        val cycled = viewModel.switchToNextCustomPreset()
        assertNotNull(cycled)
        assertEquals(sniperPreset.id, cycled?.id)
        assertEquals(sniperPreset.id, viewModel.activeConfig.value.id)
    }

    @Test
    fun `modifying equipped preset detects unsaved changes and updatePresetWithActiveConfig overwrites in place`() {
        viewModel.setReticleSizeDp(40f)
        viewModel.setPrimaryColorHex(0xFF00E676)
        viewModel.setReticleOpacity(0.85f)
        viewModel.setReticleRotationDeg(0f)
        val original = viewModel.saveConfigAsPreset(name = "Main Rifle")

        assertTrue(original.matchesVisualSettings(viewModel.activeConfig.value))

        // Tweak opacity and rotation
        viewModel.setReticleOpacity(0.50f)
        viewModel.setReticleRotationDeg(90f)
        assertFalse(original.matchesVisualSettings(viewModel.activeConfig.value))

        // Overwrite existing preset in place
        val updated = viewModel.updatePresetWithActiveConfig(original)
        assertEquals(original.id, updated.id)
        assertEquals("Main Rifle", updated.name)
        assertEquals(0.50f, updated.opacity, 0.001f)
        assertEquals(90f, updated.rotationDeg, 0.01f)
        assertEquals(1, viewModel.savedPresets.value.size)
        assertTrue(updated.matchesVisualSettings(viewModel.activeConfig.value))
    }

    @Test
    fun `renameCustomPreset and confirmDeletePreset update saved presets and active state`() {
        val preset = viewModel.saveConfigAsPreset(name = "Old Name")
        assertTrue(viewModel.renameCustomPreset(preset, "Pro Headshot"))

        assertEquals("Pro Headshot", viewModel.activeConfig.value.name)
        assertEquals("Pro Headshot", viewModel.savedPresets.value.first { it.id == preset.id }.name)

        // Blank rename rejected
        assertFalse(viewModel.renameCustomPreset(preset, "   "))

        // Delete preset
        viewModel.openDeleteConfirmDialog(preset)
        assertNotNull(viewModel.deletingPreset.value)
        viewModel.confirmDeletePreset(preset)
        assertNull(viewModel.deletingPreset.value)
        assertFalse(viewModel.savedPresets.value.any { it.id == preset.id })
    }

    @Test
    fun `openSaveDialog with custom draft saves exact draft settings without mutating until confirmed`() {
        val draft = CrosshairConfig(
            id = "design_draft",
            name = "Arctic Scope",
            style = CrosshairStyle.MIL_DOT_SNIPER,
            sizeDp = 70f,
            primaryColorHex = 0xFFD500F9,
            opacity = 0.60f,
            rotationDeg = 270f,
            isCustom = false
        )

        viewModel.openSaveDialog(draft)
        assertTrue(viewModel.showSaveDialog.value)
        assertEquals(70f, viewModel.savePresetDraftConfig.value?.sizeDp ?: 0f, 0.01f)

        val saved = viewModel.saveConfigAsPreset(
            name = "My Purple Scope",
            category = CrosshairConfig.CATEGORY_SNIPER,
            sourceConfig = viewModel.savePresetDraftConfig.value!!
        )
        assertFalse(viewModel.showSaveDialog.value)
        assertNull(viewModel.savePresetDraftConfig.value)
        assertEquals(70f, saved.sizeDp, 0.01f)
        assertEquals(0xFFD500F9, saved.primaryColorHex)
        assertEquals(0.60f, saved.opacity, 0.001f)
        assertEquals(270f, saved.rotationDeg, 0.01f)
    }

    @Test
    fun `room PresetDao persists and renames custom presets accurately`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.presetDao()
            val config = CrosshairConfig(
                id = "custom_room_1",
                name = "Arena Neon",
                style = CrosshairStyle.CYBER_HEX,
                sizeDp = 46f,
                primaryColorHex = 0xFFFFEA00,
                opacity = 0.80f,
                rotationDeg = 30f,
                isCustom = true,
                category = CrosshairConfig.CATEGORY_CUSTOM
            )
            dao.insertPreset(PresetEntity.fromCrosshairConfig(config))

            val stored = dao.getPresetById("custom_room_1")?.toCrosshairConfig()
            assertNotNull(stored)
            assertEquals("Arena Neon", stored?.name)
            assertEquals(46f, stored?.sizeDp ?: 0f, 0.01f)
            assertEquals(0xFFFFEA00, stored?.primaryColorHex)
            assertEquals(0.80f, stored?.opacity ?: 0f, 0.001f)
            assertEquals(30f, stored?.rotationDeg ?: 0f, 0.01f)

            dao.updatePresetName("custom_room_1", "Arena Gold")
            val all = dao.getAllPresets().first()
            assertEquals(1, all.size)
            assertEquals("Arena Gold", all.first().name)

            dao.deletePresetById("custom_room_1")
            assertEquals(0, dao.getCount())
        } finally {
            db.close()
        }
    }
}
