package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.AiExecutionTier
import com.example.data.ai.AiFeatureType
import com.example.data.ai.QuotaSnapshot
import com.example.data.ai.RecoveryReason
import com.example.ui.theme.MinimalAmber
import com.example.ui.theme.MinimalGreen
import com.example.ui.theme.MonoFontFamily

@Composable
fun QuotaStatusHeaderCard(
    quota: QuotaSnapshot,
    onToggleEcoMode: (Boolean) -> Unit,
    onOpenValueReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val resetCountdown = remember(quota.totalUsed, quota.dateIso) { quota.formattedResetCountdown(context) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_quota_header_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (quota.hasByokKey) {
                                stringResource(R.string.ai_plan_byok_badge)
                            } else if (quota.isSignedIn) {
                                stringResource(R.string.ai_plan_starter_badge)
                            } else {
                                stringResource(R.string.ai_plan_guest_badge)
                            },
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (quota.hasByokKey) {
                                stringResource(R.string.ai_quota_unlimited)
                            } else {
                                stringResource(
                                    R.string.ai_quota_left_compact,
                                    quota.remainingCredits,
                                    quota.effectiveDailyLimit
                                )
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = MonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = stringResource(R.string.ai_quota_reset_in, resetCountdown),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenValueReceipt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("open_value_receipt_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (quota.isSignedIn) stringResource(R.string.ai_btn_receipt)
                        else stringResource(R.string.ai_btn_unlock_more),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!quota.hasByokKey) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { quota.usageFraction },
                    color = when {
                        quota.isExhausted -> MaterialTheme.colorScheme.error
                        quota.isSoftNudgeActive -> MinimalAmber
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (quota.ecoLiteMode) MinimalGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_eco_mode_label),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = quota.ecoLiteMode,
                    onCheckedChange = onToggleEcoMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MinimalGreen
                    ),
                    modifier = Modifier.testTag("ai_eco_lite_switch")
                )
            }
        }
    }
}

/**
 * Stage 1 Proactive UX: At >= 80% Usage — Soft Nudge Banner with Eco Lite suggestion.
 */
@Composable
fun SoftNudgeBanner(
    quota: QuotaSnapshot,
    onSwitchToEcoLite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val countdown = remember(quota.totalUsed) { quota.formattedResetCountdown(context) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MinimalAmber.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, MinimalAmber.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ai_soft_nudge_banner")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MinimalAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        R.string.ai_soft_nudge_title,
                        quota.remainingCredits,
                        countdown
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!quota.ecoLiteMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ai_soft_nudge_desc),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onSwitchToEcoLite,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinimalAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("ai_soft_nudge_eco_btn")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_switch_eco_lite_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Stage 2 Proactive UX: Smart Quota Recovery Card on 429 or 100% exhaustion
 * with all 4 self-serve recovery actions in friction order.
 */
@Composable
fun SmartQuotaRecoveryCard(
    feature: AiFeatureType,
    reason: RecoveryReason,
    message: String,
    quota: QuotaSnapshot,
    canRetryEcoLite: Boolean,
    onClaimGraceCredits: () -> Unit,
    onRetryEcoLite: () -> Unit,
    onRunLocalEngine: () -> Unit,
    onOpenUnlockSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val countdown = remember(quota.totalUsed, quota.dateIso) { quota.formattedResetCountdown(context) }
    val localCtaText = when (feature) {
        AiFeatureType.ARCHITECT -> stringResource(R.string.ai_recovery_local_architect)
        AiFeatureType.VISION -> stringResource(R.string.ai_recovery_local_vision)
        AiFeatureType.COACH -> stringResource(R.string.ai_recovery_local_coach)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, MinimalAmber.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("smart_quota_recovery_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MinimalAmber.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MinimalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when (reason) {
                            RecoveryReason.QUOTA_EXHAUSTED -> stringResource(R.string.ai_recovery_title_limit)
                            RecoveryReason.RATE_LIMITED_429 -> stringResource(R.string.ai_recovery_title_429)
                            RecoveryReason.OFFLINE_OR_CLOUD_UNAVAILABLE -> stringResource(R.string.ai_recovery_title_offline)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestion 1: Claim One-Time Daily Grace Credits (+3 Free Credits)
            if (quota.canClaimGraceToday && reason == RecoveryReason.QUOTA_EXHAUSTED) {
                Button(
                    onClick = onClaimGraceCredits,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_claim_grace_btn")
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_recovery_claim_grace),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Suggestion 2: Retry with Eco Lite AI
            if (canRetryEcoLite && !quota.isExhausted) {
                Button(
                    onClick = onRetryEcoLite,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MinimalGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("ai_retry_eco_btn")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.ai_recovery_retry_eco),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Suggestion 3: Zero-Quota On-Device Local Alternative (Always available, 0 credits!)
            OutlinedButton(
                onClick = onRunLocalEngine,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ai_run_local_fallback_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = localCtaText,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suggestion 4: Visible Reset Countdown + Unlock Starter Tier / BYOK
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ai_recovery_reset_countdown, countdown),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = onOpenUnlockSheet,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ai_btn_unlock_more),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AiTierBadge(
    tier: AiExecutionTier,
    fromCache: Boolean,
    autoCascaded: Boolean,
    modifier: Modifier = Modifier
) {
    val (labelRes, accent) = when (tier) {
        AiExecutionTier.PRIMARY_CLOUD -> R.string.ai_tier_primary to MaterialTheme.colorScheme.primary
        AiExecutionTier.ECONOMY_LITE -> R.string.ai_tier_eco_lite to MinimalGreen
        AiExecutionTier.LOCAL_ENGINE -> R.string.ai_tier_local to MinimalAmber
        AiExecutionTier.BYOK_DIRECT -> R.string.ai_tier_byok to MaterialTheme.colorScheme.secondary
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(labelRes),
                fontFamily = MonoFontFamily,
                color = accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (fromCache) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_badge_cached),
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (autoCascaded) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MinimalAmber.copy(alpha = 0.16f))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_badge_auto_cascaded),
                    fontFamily = MonoFontFamily,
                    color = MinimalAmber,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Value Receipt, Guest -> Google Sign-In Free Starter Unlock, and Optional BYOK Setup Dialog.
 */
@Composable
fun ValueReceiptAndUpgradeDialog(
    quota: QuotaSnapshot,
    isAuthBusy: Boolean,
    initialByokKey: String,
    onSignInWithGoogle: () -> Unit,
    onSignOutGoogle: () -> Unit,
    onClaimGraceCredits: () -> Unit,
    onSaveByokKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showByokEditor by remember { mutableStateOf(initialByokKey.isNotBlank()) }
    var byokInput by remember(initialByokKey) { mutableStateOf(initialByokKey) }
    val countdown = remember(quota.totalUsed, quota.dateIso) { quota.formattedResetCountdown(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_value_receipt_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Personal Value Receipt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.ai_value_receipt_header),
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = MonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.ai_value_receipt_summary,
                                quota.architectUsed,
                                quota.visionUsed,
                                quota.coachUsed
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                R.string.ai_value_receipt_reset_row,
                                quota.remainingCredits,
                                quota.effectiveDailyLimit,
                                countdown
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // 2. Guest -> Google Sign-In Free Starter Tier Unlock (Zero-Dollar Conversion)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (quota.isSignedIn) {
                                    quota.userDisplayName ?: quota.userEmail ?: stringResource(R.string.ai_plan_starter_badge)
                                } else {
                                    stringResource(R.string.ai_guest_unlock_title)
                                },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (quota.isSignedIn) {
                                stringResource(R.string.ai_signed_in_starter_desc)
                            } else {
                                stringResource(R.string.ai_guest_unlock_desc)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )

                        if (!quota.isSignedIn) {
                            Button(
                                onClick = onSignInWithGoogle,
                                enabled = !isAuthBusy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("ai_google_signin_btn")
                            ) {
                                if (isAuthBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.ai_sign_in_google_btn),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = onSignOutGoogle,
                                enabled = !isAuthBusy,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("ai_google_signout_btn")
                            ) {
                                Text(
                                    text = stringResource(R.string.ai_sign_out_btn),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 3. One-time Daily Grace Credits (if still available)
                if (quota.canClaimGraceToday) {
                    OutlinedButton(
                        onClick = onClaimGraceCredits,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MinimalGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("ai_dialog_claim_grace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = MinimalGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_recovery_claim_grace),
                            color = MinimalGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. Optional BYOK (Bring Your Own Key) — Power User / Zero Dev Cost Path
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showByokEditor = !showByokEditor },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ai_byok_title),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (showByokEditor) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = showByokEditor) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_byok_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                            OutlinedTextField(
                                value = byokInput,
                                onValueChange = { byokInput = it },
                                label = { Text(stringResource(R.string.ai_byok_input_label), fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_byok_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (initialByokKey.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            byokInput = ""
                                            onSaveByokKey("")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.ai_byok_clear_btn), fontSize = 11.sp)
                                    }
                                }
                                Button(
                                    onClick = { onSaveByokKey(byokInput) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_byok_save_btn")
                                ) {
                                    Text(stringResource(R.string.ai_byok_save_btn), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_done), fontWeight = FontWeight.Bold)
            }
        }
    )
}
