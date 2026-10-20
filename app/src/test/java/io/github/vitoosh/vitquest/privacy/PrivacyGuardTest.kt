package io.github.vitoosh.vitquest.privacy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the privacy promises in README and Settings, so a new dependency or a
 * stray debug line cannot quietly break them. See docs/SECURITY_PRIVACY.md.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyGuardTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Every permission the merged manifest asks for, with why. Adding one is a reviewed decision. */
    private val allowed = setOf(
        "android.permission.health.READ_STEPS", // Health Connect, read-only daily steps
        "android.permission.ACTIVITY_RECOGNITION", // phone step counter
        "android.permission.POST_NOTIFICATIONS", // reminders
        // Added by WorkManager (reminders). No network constraint is used; nothing is sent.
        "android.permission.WAKE_LOCK",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        // Added by androidx.core to protect the app's own runtime broadcast receivers.
        "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
    )

    @Test
    fun onlyReviewedPermissionsAndNoInternet() {
        @Suppress("DEPRECATION")
        val requested = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        // WAKE_LOCK comes from WorkManager's manifest, so this proves library permissions are included.
        assertTrue("Robolectric did not read the merged manifest", "android.permission.WAKE_LOCK" in requested)
        assertTrue("VitQuest must not have internet access", "android.permission.INTERNET" !in requested)
        assertEquals("Unreviewed permissions", emptySet<String>(), requested - allowed)
    }

    @Test
    fun appDataIsKeptOutOfCloudBackups() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun appCodeDoesNotLogOrPrint() {
        // Unit tests run with the module directory as working directory.
        val sources = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("No sources found", sources.size > 50)
        val forbidden = Regex("""\bandroid\.util\.Log\b|\bLog\.[vdiwe]\(|\bprintln\(|\bprint\(|\.printStackTrace\(|\bSystem\.(out|err)\b|\bTimber\b""")
        val hits = sources.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line -> if (forbidden.containsMatchIn(line)) "${file.path}:${i + 1}: ${line.trim()}" else null }
        }
        assertEquals("Logging can leak health data", emptyList<String>(), hits)
    }
}
