package io.github.vitoosh.vitquest.core.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.vitoosh.vitquest.MainActivity
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.model.ReminderType
import java.text.NumberFormat

/** What the notification says; values are null when unknown. */
data class ReminderContent(val type: ReminderType, val currentValue: Int? = null, val targetValue: Int? = null)

object ReminderNotifications {

    fun channelId(type: ReminderType) = "reminder_${type.name.lowercase()}"

    @StringRes
    fun channelName(type: ReminderType): Int = when (type) {
        ReminderType.WATER -> R.string.reminder_water
        ReminderType.MOVEMENT -> R.string.reminder_movement
        ReminderType.WORKOUT -> R.string.reminder_workout
        ReminderType.FOOD_LOG -> R.string.reminder_food_log
        ReminderType.WEIGHT -> R.string.reminder_weight
    }

    /** True when the app may show notifications (permission granted and not blocked in settings). */
    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Creates or renames the channels (one per reminder, so each can be muted in system settings). */
    fun createChannels(context: Context) {
        val localized = localizedContext(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ReminderType.entries.forEach { type ->
            manager.createNotificationChannel(
                NotificationChannel(channelId(type), localized.getString(channelName(type)), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = localized.getString(R.string.reminder_channel_description)
                },
            )
        }
    }

    fun show(context: Context, content: ReminderContent) {
        if (!canNotify(context)) return
        val localized = localizedContext(context)
        val (title, text) = texts(localized, content)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, content.type.ordinal, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // The text can include today's water or steps. On a lock screen that hides sensitive
        // content, Android shows this generic version instead.
        val (_, genericText) = texts(localized, content.copy(currentValue = null, targetValue = null))
        val publicVersion = NotificationCompat.Builder(context, channelId(content.type))
            .setSmallIcon(R.drawable.ic_stat_vitquest)
            .setContentTitle(title)
            .setContentText(genericText)
            .build()
        val notification = NotificationCompat.Builder(context, channelId(content.type))
            .setSmallIcon(R.drawable.ic_stat_vitquest)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        try {
            // One notification id per type: a new reminder replaces the previous one instead of piling up.
            NotificationManagerCompat.from(context).notify(NOTIFICATION_BASE_ID + content.type.ordinal, notification)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call; nothing to show.
        }
    }

    private fun texts(context: Context, content: ReminderContent): Pair<String, String> {
        val number = NumberFormat.getIntegerInstance(context.resources.configuration.locales[0])
        val current = content.currentValue
        val target = content.targetValue
        return when (content.type) {
            ReminderType.WATER -> context.getString(R.string.reminder_water_title) to
                if (current != null && target != null) {
                    context.getString(R.string.reminder_water_progress, number.format(current), number.format(target))
                } else {
                    context.getString(R.string.reminder_water_text)
                }
            ReminderType.MOVEMENT -> context.getString(R.string.reminder_movement_title) to
                if (current != null) context.getString(R.string.reminder_movement_progress, number.format(current)) else context.getString(R.string.reminder_movement_text)
            ReminderType.WORKOUT -> context.getString(R.string.reminder_workout_title) to context.getString(R.string.reminder_workout_text)
            ReminderType.FOOD_LOG -> context.getString(R.string.reminder_food_log_title) to context.getString(R.string.reminder_food_log_text)
            ReminderType.WEIGHT -> context.getString(R.string.reminder_weight_title) to context.getString(R.string.reminder_weight_text)
        }
    }

    /**
     * Android 13+ applies the per-app language to the whole app; older versions
     * only to activities, so background work picks the language explicitly.
     */
    fun localizedContext(context: Context): Context {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(android.os.LocaleList.forLanguageTags(locales.toLanguageTags()))
        return context.createConfigurationContext(configuration)
    }

    private const val NOTIFICATION_BASE_ID = 4_100
}
