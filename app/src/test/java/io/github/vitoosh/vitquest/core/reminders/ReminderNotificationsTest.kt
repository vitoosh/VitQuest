package io.github.vitoosh.vitquest.core.reminders

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vitoosh.vitquest.R
import io.github.vitoosh.vitquest.core.model.ReminderType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en")
class ReminderNotificationsTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun progressStaysOffLockScreensThatHideSensitiveContent() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ReminderNotifications.createChannels(context)

        ReminderNotifications.show(context, ReminderContent(ReminderType.WATER, currentValue = 1_200, targetValue = 2_400))

        val notification = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertTrue(notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString().contains("1,200"))
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        assertEquals(
            context.getString(R.string.reminder_water_text),
            notification.publicVersion.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }
}
