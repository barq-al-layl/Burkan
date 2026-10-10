package io.github.barqallayl.burkan.feature.crash.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.core.net.toUri
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.storage.SettingsStorage
import io.github.barqallayl.burkan.designsystem.AppStyle
import io.github.barqallayl.burkan.designsystem.BurkanTheme
import io.github.barqallayl.burkan.designsystem.SeedColors
import io.github.barqallayl.burkan.designsystem.resolved
import io.github.barqallayl.burkan.feature.crash.CrashReportFile
import io.github.barqallayl.burkan.feature.settings.model.About

/**
 * The screen shown after a crash: the report, and the ways to pass it on.
 *
 * It stands apart from the rest of the app on purpose. It is built by its own constructor and reads no setting, so
 * it opens even when what crashed is the code that builds or reads those. Its look is therefore the one a fresh
 * install has on this phone, not the one the user chose.
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val report = CrashReportFile(this).read()
        setContent {
            val style = AppStyle.defaultFor(Build.MANUFACTURER)
            BurkanTheme(
                isDarkTheme = isSystemInDarkTheme(),
                seedColor = SeedColors.defaultFor(style).resolved(),
                paletteStyle = SettingsStorage.Defaults.paletteStyle.style,
                appStyle = style,
            ) {
                Surface {
                    CrashScreen(
                        report = report,
                        onShare = { report?.let(::share) },
                        onReport = ::openReportForm,
                        onClose = ::finishAndRemoveTask,
                    )
                }
            }
        }
    }

    /** As text, not as a file: it is pasted into a report, and a file would need the rest of the app to hand over. */
    private fun share(report: String) {
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, report)
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_share_subject))
        startActivity(Intent.createChooser(send, getString(R.string.crash_share_chooser)))
    }

    private fun openReportForm() {
        // No browser is no reason to crash here of all places.
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, About.REPORT_URL.toUri())) }
    }
}
