package com.samupdater.app.data.device

import android.os.Build
import com.samupdater.app.domain.DeviceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Reads model, CSC and build details from the phone. Nothing here needs a permission. */
class DeviceInfoReader(private val props: SystemProps = ShellSystemProps()) {

    // System properties can't change while the app is running, so getprop runs once per process.
    private val profile by lazy { readProps() }

    /** Spawns several getprop processes the first time, so it always runs on the IO dispatcher. */
    suspend fun read(): DeviceProfile = withContext(Dispatchers.IO) { profile }

    private fun readProps(): DeviceProfile {
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        return DeviceProfile(
            isSamsung = isSamsung,
            model = detectedModel(),
            csc = CSC_PROPS.firstNotNullOfOrNull { props.get(it)?.takeIf(CSC_PATTERN::matches) },
            pdaBuild = props.get("ro.build.PDA")?.takeIf { it.isNotBlank() },
            oneUiVersion = props.get("ro.build.version.oneui")?.let(::formatOneUi),
            androidVersion = Build.VERSION.SDK_INT.let(::androidFromSdk),
            securityPatch = Build.VERSION.SECURITY_PATCH?.takeIf { it.isNotBlank() },
        )
    }

    /** The Galaxy model this app is running on, or null on other phones. Cheap, no getprop call. */
    fun detectedModel(): String? = Build.MODEL?.uppercase()?.takeIf { it.startsWith("SM-") }

    companion object {
        private val CSC_PROPS = listOf("ro.csc.sales_code", "ro.boot.sales_code", "persist.omc.sales_code")
        private val CSC_PATTERN = Regex("^[A-Z0-9]{3}$")
        private const val ONE_UI_MAJOR_DIVISOR = 10_000
        private const val ONE_UI_MINOR_DIVISOR = 100
        private const val SDK_ANDROID_9 = 28
        private const val SDK_ANDROID_12 = 31
        private const val SDK_ANDROID_13 = 33
        private const val SDK_TO_ANDROID_OFFSET = 20

        /** ro.build.version.oneui is like 80500 for One UI 8.5. */
        internal fun formatOneUi(raw: String): String? {
            val code = raw.trim().toIntOrNull() ?: return null
            val major = code / ONE_UI_MAJOR_DIVISOR
            val minor = (code % ONE_UI_MAJOR_DIVISOR) / ONE_UI_MINOR_DIVISOR
            return if (major == 0) null else if (minor == 0) "$major" else "$major.$minor"
        }

        /** Maps the SDK level to the marketing Android version. */
        internal fun androidFromSdk(sdk: Int): Int = when {
            sdk >= SDK_ANDROID_13 -> sdk - SDK_TO_ANDROID_OFFSET
            sdk >= SDK_ANDROID_12 -> 12
            sdk >= SDK_ANDROID_9 -> sdk - (SDK_TO_ANDROID_OFFSET - 1)
            else -> 8
        }
    }
}

fun interface SystemProps {
    fun get(name: String): String?
}

/** Reads system properties through the getprop binary, which every Android build ships. */
class ShellSystemProps : SystemProps {
    override fun get(name: String): String? = try {
        val process = ProcessBuilder("getprop", name).redirectErrorStream(true).start()
        val finished = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            null
        } else {
            process.inputStream.bufferedReader().use { it.readText() }.trim().takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        android.util.Log.w("SamUpdater", "getprop $name failed", e)
        null
    }

    private companion object {
        const val TIMEOUT_MS = 1_500L
    }
}
