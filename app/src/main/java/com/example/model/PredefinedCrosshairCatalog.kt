package com.example.model

import android.content.Context
import androidx.annotation.StringRes
import com.example.R

enum class DesignCategory(@StringRes val labelRes: Int) {
    ALL(R.string.filter_shape_all),
    CLASSIC_FPS(R.string.designs_cat_classic),
    PRECISION_DOTS(R.string.designs_cat_dots),
    TACTICAL_CQB(R.string.designs_cat_tactical),
    SNIPER_SCOPES(R.string.designs_cat_sniper),
    SCIFI_HUD(R.string.designs_cat_scifi)
}

enum class CrosshairCollectionCategory(@StringRes val labelRes: Int) {
    ALL(R.string.designs_cat_all),
    FAVORITES(R.string.collection_cat_favorites),
    COMPETITIVE(R.string.collection_cat_competitive),
    MINIMALIST(R.string.collection_cat_minimalist),
    TACTICAL(R.string.collection_cat_tactical),
    FUN(R.string.collection_cat_fun),
    CUSTOM(R.string.title_presets);

    companion object {
        val PREDEFINED_TABS = listOf(ALL, FAVORITES, COMPETITIVE, MINIMALIST, TACTICAL, FUN)
        val GALLERY_TABS = listOf(ALL, FAVORITES, COMPETITIVE, MINIMALIST, TACTICAL, FUN, CUSTOM)
    }
}

data class PredefinedCrosshairDesign(
    val config: CrosshairConfig,
    val category: DesignCategory,
    val collectionCategory: CrosshairCollectionCategory,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    @StringRes val roleTagRes: Int
)

object PredefinedCrosshairCatalog {

    val DESIGNS: List<PredefinedCrosshairDesign> = listOf(
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_toxic_pro",
                style = CrosshairStyle.CLASSIC_CROSS,
                sizeDp = 34f,
                thicknessDp = 2.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFF00E676,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_CLASSIC
            ),
            category = DesignCategory.CLASSIC_FPS,
            collectionCategory = CrosshairCollectionCategory.COMPETITIVE,
            titleRes = R.string.design_toxic_cs_title,
            summaryRes = R.string.design_toxic_cs_desc,
            roleTagRes = R.string.design_role_esports
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_tournament_cyan",
                style = CrosshairStyle.CLASSIC_CROSS,
                sizeDp = 30f,
                thicknessDp = 2f,
                gapDp = 5f,
                centerDot = false,
                primaryColorHex = 0xFF00E5FF,
                outlineEnabled = true,
                outlineThicknessDp = 1.5f,
                category = CrosshairConfig.CATEGORY_CLASSIC
            ),
            category = DesignCategory.CLASSIC_FPS,
            collectionCategory = CrosshairCollectionCategory.COMPETITIVE,
            titleRes = R.string.design_cyan_comp_title,
            summaryRes = R.string.design_cyan_comp_desc,
            roleTagRes = R.string.design_role_rifles
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_pubg_t",
                style = CrosshairStyle.T_SHAPE,
                sizeDp = 38f,
                thicknessDp = 3f,
                gapDp = 8f,
                centerDot = false,
                tStyle = true,
                primaryColorHex = 0xFF76FF03,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_CLASSIC
            ),
            category = DesignCategory.CLASSIC_FPS,
            collectionCategory = CrosshairCollectionCategory.COMPETITIVE,
            titleRes = R.string.design_open_t_title,
            summaryRes = R.string.design_open_t_desc,
            roleTagRes = R.string.design_role_headshot
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_cross_circle",
                style = CrosshairStyle.CROSS_CIRCLE,
                sizeDp = 40f,
                thicknessDp = 2.2f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFFFFFFFF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_CLASSIC
            ),
            category = DesignCategory.CLASSIC_FPS,
            collectionCategory = CrosshairCollectionCategory.MINIMALIST,
            titleRes = R.string.design_cross_circle_title,
            summaryRes = R.string.design_cross_circle_desc,
            roleTagRes = R.string.design_role_allround
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_valorant_dot",
                style = CrosshairStyle.PRECISION_DOT,
                sizeDp = 24f,
                thicknessDp = 2f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 4.5f,
                primaryColorHex = 0xFF00E5FF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_DOT
            ),
            category = DesignCategory.PRECISION_DOTS,
            collectionCategory = CrosshairCollectionCategory.COMPETITIVE,
            titleRes = R.string.design_onetap_dot_title,
            summaryRes = R.string.design_onetap_dot_desc,
            roleTagRes = R.string.design_role_onetap
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_crimson_pinpoint",
                style = CrosshairStyle.PRECISION_DOT,
                sizeDp = 18f,
                thicknessDp = 1.8f,
                gapDp = 2f,
                centerDot = true,
                centerDotSizeDp = 5f,
                primaryColorHex = 0xFFFF1744,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_DOT
            ),
            category = DesignCategory.PRECISION_DOTS,
            collectionCategory = CrosshairCollectionCategory.MINIMALIST,
            titleRes = R.string.design_crimson_dot_title,
            summaryRes = R.string.design_crimson_dot_desc,
            roleTagRes = R.string.design_role_precision
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_micro_plus",
                style = CrosshairStyle.MICRO_PLUS,
                sizeDp = 22f,
                thicknessDp = 2f,
                gapDp = 0f,
                centerDot = false,
                primaryColorHex = 0xFFFFFFFF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_DOT
            ),
            category = DesignCategory.PRECISION_DOTS,
            collectionCategory = CrosshairCollectionCategory.MINIMALIST,
            titleRes = R.string.design_micro_plus_title,
            summaryRes = R.string.design_micro_plus_desc,
            roleTagRes = R.string.design_role_micro
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_diamond_dot",
                style = CrosshairStyle.HOLLOW_DIAMOND,
                sizeDp = 32f,
                thicknessDp = 2.5f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFFFFEA00,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_DOT
            ),
            category = DesignCategory.PRECISION_DOTS,
            collectionCategory = CrosshairCollectionCategory.MINIMALIST,
            titleRes = R.string.design_hollow_diamond_title,
            summaryRes = R.string.design_hollow_diamond_desc,
            roleTagRes = R.string.design_role_flicks
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_warzone_circle",
                style = CrosshairStyle.TACTICAL_CIRCLE,
                sizeDp = 44f,
                thicknessDp = 2.5f,
                gapDp = 10f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFFFFEA00,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_TACTICAL
            ),
            category = DesignCategory.TACTICAL_CQB,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_tactical_ring_title,
            summaryRes = R.string.design_tactical_ring_desc,
            roleTagRes = R.string.design_role_tracking
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_apex_chevron",
                style = CrosshairStyle.CHEVRON_ARROW,
                sizeDp = 38f,
                thicknessDp = 3.5f,
                gapDp = 6f,
                centerDot = false,
                primaryColorHex = 0xFFFF9100,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_TACTICAL
            ),
            category = DesignCategory.TACTICAL_CQB,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_chevron_title,
            summaryRes = R.string.design_chevron_desc,
            roleTagRes = R.string.design_role_drop_lead
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_shotgun_cqb",
                style = CrosshairStyle.SHOTGUN_BOX,
                sizeDp = 46f,
                thicknessDp = 3f,
                gapDp = 10f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFF00E5FF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_TACTICAL
            ),
            category = DesignCategory.TACTICAL_CQB,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_shotgun_box_title,
            summaryRes = R.string.design_shotgun_box_desc,
            roleTagRes = R.string.design_role_cqb
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_corner_brackets",
                style = CrosshairStyle.CORNER_BRACKETS,
                sizeDp = 44f,
                thicknessDp = 2.8f,
                gapDp = 8f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFF00E676,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_TACTICAL
            ),
            category = DesignCategory.TACTICAL_CQB,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_corner_brackets_title,
            summaryRes = R.string.design_corner_brackets_desc,
            roleTagRes = R.string.design_role_hipfire
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_dynamic_ring",
                style = CrosshairStyle.DYNAMIC_RING,
                sizeDp = 40f,
                thicknessDp = 3.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFFFF5252,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_TACTICAL
            ),
            category = DesignCategory.TACTICAL_CQB,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_segmented_ring_title,
            summaryRes = R.string.design_segmented_ring_desc,
            roleTagRes = R.string.design_role_smg
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_cod_sniper",
                style = CrosshairStyle.MIL_DOT_SNIPER,
                sizeDp = 48f,
                thicknessDp = 2.5f,
                gapDp = 12f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFFFF1744,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SNIPER
            ),
            category = DesignCategory.SNIPER_SCOPES,
            collectionCategory = CrosshairCollectionCategory.COMPETITIVE,
            titleRes = R.string.design_mildot_sniper_title,
            summaryRes = R.string.design_mildot_sniper_desc,
            roleTagRes = R.string.design_role_sniper
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_arctic_sniper",
                style = CrosshairStyle.MIL_DOT_SNIPER,
                sizeDp = 52f,
                thicknessDp = 2f,
                gapDp = 10f,
                centerDot = true,
                centerDotSizeDp = 3f,
                rotationDeg = 45f,
                primaryColorHex = 0xFF40C4FF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SNIPER
            ),
            category = DesignCategory.SNIPER_SCOPES,
            collectionCategory = CrosshairCollectionCategory.TACTICAL,
            titleRes = R.string.design_arctic_sniper_title,
            summaryRes = R.string.design_arctic_sniper_desc,
            roleTagRes = R.string.design_role_long_range
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_cyber_hex",
                style = CrosshairStyle.CYBER_HEX,
                sizeDp = 46f,
                thicknessDp = 2.5f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 3f,
                primaryColorHex = 0xFFD500F9,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SCIFI
            ),
            category = DesignCategory.SCIFI_HUD,
            collectionCategory = CrosshairCollectionCategory.FUN,
            titleRes = R.string.design_cyber_hex_title,
            summaryRes = R.string.design_cyber_hex_desc,
            roleTagRes = R.string.design_role_scifi
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_tri_blade",
                style = CrosshairStyle.TRI_BLADE,
                sizeDp = 42f,
                thicknessDp = 3f,
                gapDp = 6f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFF76FF03,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SCIFI
            ),
            category = DesignCategory.SCIFI_HUD,
            collectionCategory = CrosshairCollectionCategory.FUN,
            titleRes = R.string.design_tri_blade_title,
            summaryRes = R.string.design_tri_blade_desc,
            roleTagRes = R.string.design_role_arena
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "preset_sonar_radar",
                style = CrosshairStyle.SONAR_RADAR,
                sizeDp = 50f,
                thicknessDp = 2f,
                gapDp = 8f,
                centerDot = true,
                centerDotSizeDp = 3.5f,
                primaryColorHex = 0xFF40C4FF,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SCIFI
            ),
            category = DesignCategory.SCIFI_HUD,
            collectionCategory = CrosshairCollectionCategory.FUN,
            titleRes = R.string.design_sonar_radar_title,
            summaryRes = R.string.design_sonar_radar_desc,
            roleTagRes = R.string.design_role_hud
        ),
        PredefinedCrosshairDesign(
            config = CrosshairConfig(
                id = "design_pulse_heartbeat",
                style = CrosshairStyle.HEARTBEAT,
                sizeDp = 46f,
                thicknessDp = 2.5f,
                gapDp = 4f,
                centerDot = true,
                centerDotSizeDp = 4f,
                primaryColorHex = 0xFFFF5252,
                outlineEnabled = true,
                category = CrosshairConfig.CATEGORY_SCIFI
            ),
            category = DesignCategory.SCIFI_HUD,
            collectionCategory = CrosshairCollectionCategory.FUN,
            titleRes = R.string.design_pulse_cross_title,
            summaryRes = R.string.design_pulse_cross_desc,
            roleTagRes = R.string.design_role_cyber
        )
    )

    private val titleResById: Map<String, Int> = DESIGNS.associate { it.config.id to it.titleRes }
    private val collectionCategoryById: Map<String, CrosshairCollectionCategory> =
        DESIGNS.associate { it.config.id to it.collectionCategory }

    @StringRes
    fun resolveTitleResById(id: String): Int? = titleResById[id]

    fun resolveCollectionCategory(config: CrosshairConfig): CrosshairCollectionCategory {
        collectionCategoryById[config.id]?.let { return it }
        return when (config.style) {
            CrosshairStyle.PRECISION_DOT,
            CrosshairStyle.MICRO_PLUS,
            CrosshairStyle.HOLLOW_DIAMOND -> CrosshairCollectionCategory.MINIMALIST

            CrosshairStyle.CLASSIC_CROSS,
            CrosshairStyle.T_SHAPE,
            CrosshairStyle.CROSS_CIRCLE,
            CrosshairStyle.MIL_DOT_SNIPER -> CrosshairCollectionCategory.COMPETITIVE

            CrosshairStyle.TACTICAL_CIRCLE,
            CrosshairStyle.CHEVRON_ARROW,
            CrosshairStyle.SHOTGUN_BOX,
            CrosshairStyle.CORNER_BRACKETS,
            CrosshairStyle.DYNAMIC_RING -> CrosshairCollectionCategory.TACTICAL

            CrosshairStyle.CYBER_HEX,
            CrosshairStyle.TRI_BLADE,
            CrosshairStyle.SONAR_RADAR,
            CrosshairStyle.HEARTBEAT -> CrosshairCollectionCategory.FUN
        }
    }

    fun matchesCollectionCategory(
        config: CrosshairConfig,
        category: CrosshairCollectionCategory,
        favoriteIds: Set<String> = emptySet()
    ): Boolean = when (category) {
        CrosshairCollectionCategory.ALL -> true
        CrosshairCollectionCategory.FAVORITES -> config.id in favoriteIds
        CrosshairCollectionCategory.CUSTOM -> config.isCustom
        else -> resolveCollectionCategory(config) == category
    }

    fun defaultConfig(context: Context): CrosshairConfig {
        val first = DESIGNS.first()
        return first.config.copy(name = context.getString(first.titleRes))
    }

    fun resolveDefaultConfig(config: CrosshairConfig): CrosshairConfig {
        val byId = DESIGNS.firstOrNull { it.config.id == config.id }?.config
        val byStyle = DESIGNS.firstOrNull { it.config.style == config.style }?.config
        return byId ?: byStyle ?: DESIGNS.first().config
    }

    fun localizedPresets(context: Context): List<CrosshairConfig> {
        return DESIGNS.map { design ->
            design.config.copy(name = context.getString(design.titleRes))
        }
    }

    fun resolveDisplayName(context: Context, config: CrosshairConfig): String {
        val builtInTitleRes = resolveTitleResById(config.id)
        return when {
            builtInTitleRes != null -> context.getString(builtInTitleRes)
            config.name.isNotBlank() -> config.name
            else -> context.getString(R.string.default_custom_crosshair_name)
        }
    }
}
