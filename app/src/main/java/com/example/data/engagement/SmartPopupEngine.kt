package com.example.data.engagement

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.analytics.AnalyticsSummary
import com.example.data.analytics.AppEventTracker
import com.example.data.analytics.AppEventType

enum class EngagementMoment(val key: String) {
    AIM_DRILL_HIGH_SCORE("aim_drill_high_score"),
    PRESET_SAVED("preset_saved"),
    PRESET_SHARED_OR_IMPORTED("preset_shared_or_imported"),
    OVERLAY_SESSION_COMPLETED("overlay_session_completed"),
    RETURNING_USER_SETTLED("returning_user_settled")
}

data class UpdateMetadata(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val releaseNotes: String = ""
)

sealed class SmartPopupProposal(val analyticsKey: String) {
    data class UpdateApp(
        val currentVersionName: String,
        val latestVersionName: String,
        val latestVersionCode: Int,
        val releaseNotes: String
    ) : SmartPopupProposal("update_app")

    data class RateApp(
        val moment: EngagementMoment
    ) : SmartPopupProposal("rate_app")

    data class ShareApp(
        val moment: EngagementMoment
    ) : SmartPopupProposal("share_app")
}

data class EngagementContext(
    val moment: EngagementMoment,
    val hasCompletedOnboarding: Boolean,
    val isSplashVisible: Boolean,
    val isAimDrillRunning: Boolean,
    val hasOtherDialogOpen: Boolean,
    val currentScreen: String,
    val savedPresetsCount: Int
)

/**
 * Anti-spam, context-aware auto-popup scheduler for Update, Rate App, and Share App suggestions.
 *
 * Core safeguards enforced deterministically:
 *  - Never triggers at cold app launch (requires >= 40s active session uptime).
 *  - Never interrupts splash screen, onboarding, active aim drill, or another open dialog.
 *  - Strictly max 1 auto-popup per app session.
 *  - Strictly >= 48 hours global cooldown between ANY two auto-popups across all types.
 *  - Exponential backoff per popup type + hard lifetime dismissal cap (3 dismissals -> auto-mute).
 *  - Requires multi-session engagement & positive user moments before asking for Rate or Share.
 *  - Never asks users who gave a low rating (1-3 stars) to Share the app.
 */
class SmartPopupEngine private constructor(
    context: Context,
    private val tracker: AppEventTracker
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var hasShownPopupThisSession: Boolean = false

    fun registerAvailableUpdate(metadata: UpdateMetadata) {
        prefs.edit()
            .putInt(KEY_AVAILABLE_VERSION_CODE, metadata.latestVersionCode)
            .putString(KEY_AVAILABLE_VERSION_NAME, metadata.latestVersionName)
            .putString(KEY_AVAILABLE_RELEASE_NOTES, metadata.releaseNotes)
            .putLong(KEY_LAST_UPDATE_CHECK_MS, System.currentTimeMillis())
            .apply()
    }

    fun getAvailableUpdate(): UpdateMetadata? {
        val code = prefs.getInt(KEY_AVAILABLE_VERSION_CODE, BuildConfig.VERSION_CODE)
        if (code <= BuildConfig.VERSION_CODE) return null
        val name = prefs.getString(KEY_AVAILABLE_VERSION_NAME, null) ?: return null
        val notes = prefs.getString(KEY_AVAILABLE_RELEASE_NOTES, "") ?: ""
        return UpdateMetadata(code, name, notes)
    }

    fun hasNewerUpdateAvailable(): Boolean = getAvailableUpdate() != null

    fun evaluateBestPopup(
        ctx: EngagementContext,
        nowMs: Long = System.currentTimeMillis()
    ): SmartPopupProposal? {
        // 1. Hard UX & anti-interruption guards
        if (!ctx.hasCompletedOnboarding) {
            logSuppressed(ctx, "onboarding_incomplete")
            return null
        }
        if (ctx.isSplashVisible || ctx.isAimDrillRunning || ctx.hasOtherDialogOpen) {
            logSuppressed(ctx, "user_busy_or_dialog_open")
            return null
        }
        if (hasShownPopupThisSession) {
            logSuppressed(ctx, "session_cap_reached")
            return null
        }

        // 2. Anti-cold-start guard: NEVER pop up right when the user opens the app
        val uptimeMs = tracker.getSessionUptimeMs()
        if (uptimeMs < MIN_SESSION_UPTIME_MS) {
            logSuppressed(ctx, "cold_start_guard")
            return null
        }

        // 3. Strict Global Cooldown between ANY two popups (48 hours)
        val lastAnyPopupMs = prefs.getLong(KEY_LAST_ANY_POPUP_MS, 0L)
        if (lastAnyPopupMs > 0L && (nowMs - lastAnyPopupMs) < GLOBAL_POPUP_COOLDOWN_MS) {
            logSuppressed(ctx, "global_cooldown_active")
            return null
        }

        val summary = tracker.getCurrentSummarySnapshot()
        val engagementScore = computeEngagementScore(summary, ctx.savedPresetsCount)

        // 4. Score eligible candidates and pick the highest-priority optimal suggestion
        val candidates = mutableListOf<Pair<Int, SmartPopupProposal>>()

        evaluateUpdateCandidate(ctx, nowMs)?.let { candidates.add(it) }
        evaluateRateCandidate(ctx, summary, engagementScore, nowMs)?.let { candidates.add(it) }
        evaluateShareCandidate(ctx, summary, engagementScore, nowMs)?.let { candidates.add(it) }

        val winner = candidates.maxByOrNull { it.first }?.second
        if (winner == null) {
            logSuppressed(ctx, "no_eligible_candidate")
            return null
        }

        markPopupPresented(winner, ctx.currentScreen, nowMs)
        return winner
    }

    private fun evaluateUpdateCandidate(
        ctx: EngagementContext,
        nowMs: Long
    ): Pair<Int, SmartPopupProposal>? {
        val update = getAvailableUpdate() ?: return null
        val skippedVersion = prefs.getInt(KEY_SKIPPED_UPDATE_VERSION, -1)
        if (skippedVersion == update.latestVersionCode) return null

        val shownForVersion = prefs.getInt(KEY_UPDATE_SHOWN_COUNT_FOR_VERSION, 0)
        val trackedVersion = prefs.getInt(KEY_UPDATE_TRACKED_VERSION, -1)
        val effectiveCount = if (trackedVersion == update.latestVersionCode) shownForVersion else 0
        if (effectiveCount >= MAX_UPDATE_PROMPTS_PER_VERSION) return null

        val lastDismissedMs = prefs.getLong(KEY_UPDATE_LAST_SHOWN_MS, 0L)
        if (lastDismissedMs > 0L && (nowMs - lastDismissedMs) < UPDATE_COOLDOWN_MS) return null

        // High priority when a real newer version exists, on any calm transition
        val score = 100 + when (ctx.moment) {
            EngagementMoment.RETURNING_USER_SETTLED -> 10
            EngagementMoment.OVERLAY_SESSION_COMPLETED -> 5
            else -> 0
        }
        return score to SmartPopupProposal.UpdateApp(
            currentVersionName = BuildConfig.VERSION_NAME,
            latestVersionName = update.latestVersionName,
            latestVersionCode = update.latestVersionCode,
            releaseNotes = update.releaseNotes
        )
    }

    private fun evaluateRateCandidate(
        ctx: EngagementContext,
        summary: AnalyticsSummary,
        engagementScore: Int,
        nowMs: Long
    ): Pair<Int, SmartPopupProposal>? {
        // NEVER trigger Rate App on passive open/settled state — only on positive milestones
        if (ctx.moment == EngagementMoment.RETURNING_USER_SETTLED) return null

        if (prefs.getBoolean(KEY_HAS_RATED, false)) return null
        if (prefs.getBoolean(KEY_NEVER_ASK_RATE, false)) return null

        val dismissCount = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0)
        if (dismissCount >= MAX_RATE_DISMISSALS) return null

        // Exponential backoff: 1st dismiss -> 5 days, 2nd dismiss -> 14 days
        val requiredBackoffMs = when (dismissCount) {
            0 -> 0L
            1 -> 5L * DAY_MS
            else -> 14L * DAY_MS
        }
        val lastRateShownMs = prefs.getLong(KEY_RATE_LAST_SHOWN_MS, 0L)
        if (lastRateShownMs > 0L && (nowMs - lastRateShownMs) < requiredBackoffMs) return null

        // Multi-session maturity gate: never on 1st or 2nd open
        if (summary.appOpenCount < MIN_OPENS_FOR_RATE) return null

        val appAgeMs = (nowMs - summary.firstInstallTimeMs).coerceAtLeast(0L)
        val isMatureEnough = appAgeMs >= MIN_INSTALL_AGE_FOR_RATE_MS || engagementScore >= POWER_USER_ENGAGEMENT_SCORE
        if (!isMatureEnough) return null

        if (engagementScore < MIN_ENGAGEMENT_FOR_RATE) return null

        val momentBonus = when (ctx.moment) {
            EngagementMoment.AIM_DRILL_HIGH_SCORE -> 20
            EngagementMoment.PRESET_SAVED -> 16
            EngagementMoment.OVERLAY_SESSION_COMPLETED -> 14
            EngagementMoment.PRESET_SHARED_OR_IMPORTED -> 12
            EngagementMoment.RETURNING_USER_SETTLED -> 0
        }
        return (60 + momentBonus + engagementScore.coerceAtMost(15)) to SmartPopupProposal.RateApp(ctx.moment)
    }

    private fun evaluateShareCandidate(
        ctx: EngagementContext,
        summary: AnalyticsSummary,
        engagementScore: Int,
        nowMs: Long
    ): Pair<Int, SmartPopupProposal>? {
        // Only trigger Share on joyful / social moments
        if (ctx.moment == EngagementMoment.RETURNING_USER_SETTLED) return null

        if (prefs.getBoolean(KEY_HAS_SHARED_APP, false)) return null
        if (prefs.getBoolean(KEY_NEVER_ASK_SHARE, false)) return null

        // Never nag dissatisfied users to share the app
        val lastRating = prefs.getInt(KEY_LAST_USER_RATING, 0)
        if (lastRating in 1..3) return null

        val dismissCount = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0)
        if (dismissCount >= MAX_SHARE_DISMISSALS) return null

        // Exponential backoff: 1st dismiss -> 7 days, 2nd dismiss -> 21 days
        val requiredBackoffMs = when (dismissCount) {
            0 -> 0L
            1 -> 7L * DAY_MS
            else -> 21L * DAY_MS
        }
        val lastShareShownMs = prefs.getLong(KEY_SHARE_LAST_SHOWN_MS, 0L)
        if (lastShareShownMs > 0L && (nowMs - lastShareShownMs) < requiredBackoffMs) return null

        if (summary.appOpenCount < MIN_OPENS_FOR_SHARE) return null
        if (engagementScore < MIN_ENGAGEMENT_FOR_SHARE) return null

        val momentBonus = when (ctx.moment) {
            EngagementMoment.PRESET_SHARED_OR_IMPORTED -> 24
            EngagementMoment.AIM_DRILL_HIGH_SCORE -> 18
            EngagementMoment.PRESET_SAVED -> 15
            EngagementMoment.OVERLAY_SESSION_COMPLETED -> 10
            EngagementMoment.RETURNING_USER_SETTLED -> 0
        }
        val ratedBonus = if (lastRating >= 4) 15 else 0
        return (50 + momentBonus + ratedBonus) to SmartPopupProposal.ShareApp(ctx.moment)
    }

    fun computeEngagementScore(summary: AnalyticsSummary, savedPresetsCount: Int): Int {
        val presets = maxOf(summary.customPresetsSaved, savedPresetsCount)
        return (summary.meaningfulOverlaySessions * 3) +
            (summary.aimDrillsCompleted * 2) +
            (summary.highScoreDrillsCount * 2) +
            (presets * 3) +
            (summary.presetsSharedCount * 2) +
            summary.appOpenCount.coerceAtMost(5)
    }

    private fun markPopupPresented(proposal: SmartPopupProposal, screen: String, nowMs: Long) {
        hasShownPopupThisSession = true
        val editor = prefs.edit().putLong(KEY_LAST_ANY_POPUP_MS, nowMs)

        when (proposal) {
            is SmartPopupProposal.UpdateApp -> {
                val trackedVersion = prefs.getInt(KEY_UPDATE_TRACKED_VERSION, -1)
                val prevCount = if (trackedVersion == proposal.latestVersionCode) {
                    prefs.getInt(KEY_UPDATE_SHOWN_COUNT_FOR_VERSION, 0)
                } else 0
                editor
                    .putInt(KEY_UPDATE_TRACKED_VERSION, proposal.latestVersionCode)
                    .putInt(KEY_UPDATE_SHOWN_COUNT_FOR_VERSION, prevCount + 1)
                    .putLong(KEY_UPDATE_LAST_SHOWN_MS, nowMs)
            }
            is SmartPopupProposal.RateApp -> {
                editor.putLong(KEY_RATE_LAST_SHOWN_MS, nowMs)
            }
            is SmartPopupProposal.ShareApp -> {
                editor.putLong(KEY_SHARE_LAST_SHOWN_MS, nowMs)
            }
        }
        editor.apply()

        tracker.logEvent(
            type = AppEventType.POPUP_IMPRESSION,
            screen = screen,
            params = mapOf("popup_type" to proposal.analyticsKey)
        )
    }

    // --- User Action Handlers ---

    fun onUpdateAccepted(versionCode: Int, screen: String) {
        tracker.logEvent(
            type = AppEventType.POPUP_ACCEPTED,
            screen = screen,
            params = mapOf("popup_type" to "update_app", "target_version" to versionCode)
        )
    }

    fun onUpdateDismissedLater(screen: String) {
        prefs.edit().putLong(KEY_UPDATE_LAST_SHOWN_MS, System.currentTimeMillis()).apply()
        tracker.logEvent(
            type = AppEventType.POPUP_DISMISSED,
            screen = screen,
            params = mapOf("popup_type" to "update_app", "action" to "later")
        )
    }

    fun onUpdateSkippedVersion(versionCode: Int, screen: String) {
        prefs.edit().putInt(KEY_SKIPPED_UPDATE_VERSION, versionCode).apply()
        tracker.logEvent(
            type = AppEventType.POPUP_NEVER_ASK,
            screen = screen,
            params = mapOf("popup_type" to "update_app", "skipped_version" to versionCode)
        )
    }

    fun onRateSubmitted(stars: Int, screen: String) {
        prefs.edit()
            .putBoolean(KEY_HAS_RATED, true)
            .putInt(KEY_LAST_USER_RATING, stars)
            .apply()
        tracker.logEvent(
            type = AppEventType.POPUP_ACCEPTED,
            screen = screen,
            params = mapOf("popup_type" to "rate_app", "stars" to stars)
        )
    }

    fun onRateDismissedLater(screen: String) {
        val nextDismiss = prefs.getInt(KEY_RATE_DISMISS_COUNT, 0) + 1
        prefs.edit()
            .putInt(KEY_RATE_DISMISS_COUNT, nextDismiss)
            .putLong(KEY_RATE_LAST_SHOWN_MS, System.currentTimeMillis())
            .apply()
        tracker.logEvent(
            type = AppEventType.POPUP_DISMISSED,
            screen = screen,
            params = mapOf("popup_type" to "rate_app", "dismiss_count" to nextDismiss)
        )
    }

    fun onRateNeverAskAgain(screen: String) {
        prefs.edit().putBoolean(KEY_NEVER_ASK_RATE, true).apply()
        tracker.logEvent(
            type = AppEventType.POPUP_NEVER_ASK,
            screen = screen,
            params = mapOf("popup_type" to "rate_app")
        )
    }

    fun onShareAccepted(screen: String) {
        prefs.edit().putBoolean(KEY_HAS_SHARED_APP, true).apply()
        tracker.logEvent(
            type = AppEventType.POPUP_ACCEPTED,
            screen = screen,
            params = mapOf("popup_type" to "share_app")
        )
    }

    fun onShareDismissedLater(screen: String) {
        val nextDismiss = prefs.getInt(KEY_SHARE_DISMISS_COUNT, 0) + 1
        prefs.edit()
            .putInt(KEY_SHARE_DISMISS_COUNT, nextDismiss)
            .putLong(KEY_SHARE_LAST_SHOWN_MS, System.currentTimeMillis())
            .apply()
        tracker.logEvent(
            type = AppEventType.POPUP_DISMISSED,
            screen = screen,
            params = mapOf("popup_type" to "share_app", "dismiss_count" to nextDismiss)
        )
    }

    fun onShareNeverAskAgain(screen: String) {
        prefs.edit().putBoolean(KEY_NEVER_ASK_SHARE, true).apply()
        tracker.logEvent(
            type = AppEventType.POPUP_NEVER_ASK,
            screen = screen,
            params = mapOf("popup_type" to "share_app")
        )
    }

    private fun logSuppressed(ctx: EngagementContext, reason: String) {
        tracker.logEvent(
            type = AppEventType.POPUP_SUPPRESSED,
            screen = ctx.currentScreen,
            params = mapOf("moment" to ctx.moment.key, "reason" to reason)
        )
    }

    companion object {
        private const val PREFS_NAME = "trueaim_smart_popup_prefs"
        private const val HOUR_MS = 3_600_000L
        private const val DAY_MS = 24L * HOUR_MS

        // Anti-spam & timing constants
        const val MIN_SESSION_UPTIME_MS = 40_000L
        const val GLOBAL_POPUP_COOLDOWN_MS = 48L * HOUR_MS
        const val UPDATE_COOLDOWN_MS = 7L * DAY_MS
        const val MIN_INSTALL_AGE_FOR_RATE_MS = 24L * HOUR_MS

        const val MIN_OPENS_FOR_RATE = 3
        const val MIN_OPENS_FOR_SHARE = 4
        const val MIN_ENGAGEMENT_FOR_RATE = 7
        const val MIN_ENGAGEMENT_FOR_SHARE = 10
        const val POWER_USER_ENGAGEMENT_SCORE = 11

        const val MAX_RATE_DISMISSALS = 3
        const val MAX_SHARE_DISMISSALS = 3
        const val MAX_UPDATE_PROMPTS_PER_VERSION = 2

        private const val KEY_LAST_ANY_POPUP_MS = "last_any_popup_ms"
        private const val KEY_HAS_RATED = "has_rated_app"
        private const val KEY_NEVER_ASK_RATE = "never_ask_rate"
        private const val KEY_RATE_DISMISS_COUNT = "rate_dismiss_count"
        private const val KEY_RATE_LAST_SHOWN_MS = "rate_last_shown_ms"
        private const val KEY_LAST_USER_RATING = "last_user_rating"

        private const val KEY_HAS_SHARED_APP = "has_shared_app"
        private const val KEY_NEVER_ASK_SHARE = "never_ask_share"
        private const val KEY_SHARE_DISMISS_COUNT = "share_dismiss_count"
        private const val KEY_SHARE_LAST_SHOWN_MS = "share_last_shown_ms"

        private const val KEY_AVAILABLE_VERSION_CODE = "available_version_code"
        private const val KEY_AVAILABLE_VERSION_NAME = "available_version_name"
        private const val KEY_AVAILABLE_RELEASE_NOTES = "available_release_notes"
        private const val KEY_LAST_UPDATE_CHECK_MS = "last_update_check_ms"
        private const val KEY_SKIPPED_UPDATE_VERSION = "skipped_update_version"
        private const val KEY_UPDATE_TRACKED_VERSION = "update_tracked_version"
        private const val KEY_UPDATE_SHOWN_COUNT_FOR_VERSION = "update_shown_count_for_version"
        private const val KEY_UPDATE_LAST_SHOWN_MS = "update_last_shown_ms"

        @Volatile
        private var INSTANCE: SmartPopupEngine? = null

        fun getInstance(context: Context): SmartPopupEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartPopupEngine(
                    context.applicationContext,
                    AppEventTracker.getInstance(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
}
