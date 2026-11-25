package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.model.CrosshairConfig
import com.example.model.CrosshairStyle
import com.example.ui.components.CustomPresetsQuickSwitchSection
import com.example.ui.components.DeletePresetConfirmDialog
import com.example.ui.components.RenamePresetDialog
import com.example.ui.components.SavePresetDialog
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xhdpi", sdk = [36])
class CustomPresetUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `SavePresetDialog displays size color opacity rotation and validates preset name`() {
        val sampleConfig = CrosshairConfig(
            id = "preset_toxic_pro",
            name = "Toxic CS Pro",
            style = CrosshairStyle.CLASSIC_CROSS,
            sizeDp = 48f,
            primaryColorHex = 0xFF00E5FF,
            opacity = 0.75f,
            rotationDeg = 45f,
            isCustom = false
        )
        var savedName: String? = null
        var savedCategory: String? = null
        var savedOverwriteId: String? = "sentinel"

        composeTestRule.setContent {
            MyApplicationTheme {
                SavePresetDialog(
                    config = sampleConfig,
                    existingPresets = emptyList(),
                    onDismiss = {},
                    onSave = { name, cat, overwriteId ->
                        savedName = name
                        savedCategory = cat
                        savedOverwriteId = overwriteId
                    }
                )
            }
        }

        // Verify all 4 configuration parameters are displayed in preview summary
        composeTestRule.onNodeWithTag("save_dialog_spec_size").assertIsDisplayed().assertTextContains("48 dp", substring = true)
        composeTestRule.onNodeWithTag("save_dialog_spec_color").assertIsDisplayed().assertTextContains("#00E5FF", substring = true)
        composeTestRule.onNodeWithTag("save_dialog_spec_opacity").assertIsDisplayed().assertTextContains("75%", substring = true)
        composeTestRule.onNodeWithTag("save_dialog_spec_rotation").assertIsDisplayed().assertTextContains("45°", substring = true)

        // Tapping Save with empty name shows validation error and does not save
        composeTestRule.onNodeWithTag("save_preset_confirm_btn").performClick()
        composeTestRule.onNodeWithTag("preset_name_error", useUnmergedTree = true).assertIsDisplayed()
        assertNull(savedName)

        // Enter custom name and save
        composeTestRule.onNodeWithTag("preset_name_input").performTextInput("Sniper Cyan 45")
        composeTestRule.onNodeWithTag("save_preset_confirm_btn").performClick()

        assertEquals("Sniper Cyan 45", savedName)
        assertEquals(CrosshairConfig.CATEGORY_CUSTOM, savedCategory)
        assertNull(savedOverwriteId)
    }

    @Test
    fun `SavePresetDialog supports overwriting existing custom preset`() {
        val existingPreset = CrosshairConfig(
            id = "custom_100",
            name = "Main Rifle",
            style = CrosshairStyle.CLASSIC_CROSS,
            sizeDp = 42f,
            primaryColorHex = 0xFF00E676,
            opacity = 0.90f,
            rotationDeg = 0f,
            isCustom = true
        )
        var capturedOverwriteId: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                SavePresetDialog(
                    config = existingPreset.copy(rotationDeg = 90f),
                    existingPresets = listOf(existingPreset),
                    onDismiss = {},
                    onSave = { _, _, overwriteId ->
                        capturedOverwriteId = overwriteId
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("save_mode_overwrite_btn").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithTag("save_preset_confirm_btn").performClick()

        assertEquals("custom_100", capturedOverwriteId)
    }

    @Test
    fun `CustomPresetsQuickSwitchSection allows one-tap switching and shows modified banner`() {
        val presetA = CrosshairConfig(
            id = "custom_a",
            name = "Sniper 45",
            style = CrosshairStyle.MIL_DOT_SNIPER,
            sizeDp = 52f,
            primaryColorHex = 0xFF00E5FF,
            opacity = 0.70f,
            rotationDeg = 45f,
            isCustom = true
        )
        val presetB = CrosshairConfig(
            id = "custom_b",
            name = "CQB Red",
            style = CrosshairStyle.SHOTGUN_BOX,
            sizeDp = 32f,
            primaryColorHex = 0xFFFF1744,
            opacity = 1.0f,
            rotationDeg = 0f,
            isCustom = true
        )
        // Active config is presetA with modified rotation
        val modifiedActive = presetA.copy(rotationDeg = 90f)
        var selectedPreset: CrosshairConfig? = null
        var updatedPreset: CrosshairConfig? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                CustomPresetsQuickSwitchSection(
                    presets = listOf(presetA, presetB),
                    activeConfig = modifiedActive,
                    onSelectPreset = { selectedPreset = it },
                    onSaveCurrent = {},
                    onUpdatePreset = { updatedPreset = it },
                    onRenamePreset = {},
                    onDeletePreset = {},
                    onViewAll = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("quick_preset_card_custom_a").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_preset_name_custom_a", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("Sniper 45")
        composeTestRule.onNodeWithTag("quick_preset_card_custom_b").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_preset_name_custom_b", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("CQB Red")
        composeTestRule.onNodeWithTag("quick_presets_modified_banner").assertIsDisplayed()

        // Tap Update button in modified banner
        composeTestRule.onNodeWithTag("quick_preset_update_modified_btn").performClick()
        assertEquals("custom_a", updatedPreset?.id)

        // Tap preset B card for instant one-tap switching
        composeTestRule.onNodeWithTag("quick_preset_card_custom_b").performScrollTo().performClick()
        assertEquals("custom_b", selectedPreset?.id)
    }

    @Test
    fun `RenamePresetDialog invokes callback with trimmed name`() {
        val preset = CrosshairConfig(
            id = "custom_rename",
            name = "Old Name",
            style = CrosshairStyle.PRECISION_DOT,
            sizeDp = 24f,
            primaryColorHex = 0xFFFFEA00,
            opacity = 0.85f,
            rotationDeg = 0f,
            isCustom = true
        )
        var renamedTo: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                RenamePresetDialog(
                    preset = preset,
                    onDismiss = {},
                    onRename = { renamedTo = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("rename_preset_input").performTextClearance()
        composeTestRule.onNodeWithTag("rename_preset_input").performTextInput("Gold Pinpoint")
        composeTestRule.onNodeWithTag("rename_preset_confirm_btn").performClick()
        assertEquals("Gold Pinpoint", renamedTo)
    }

    @Test
    fun `DeletePresetConfirmDialog confirms deletion`() {
        val preset = CrosshairConfig(
            id = "custom_del",
            name = "To Delete",
            isCustom = true
        )
        var confirmed = false

        composeTestRule.setContent {
            MyApplicationTheme {
                DeletePresetConfirmDialog(
                    preset = preset,
                    onDismiss = {},
                    onConfirmDelete = { confirmed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("delete_preset_confirm_btn").performClick()
        assertTrue(confirmed)
    }
}
