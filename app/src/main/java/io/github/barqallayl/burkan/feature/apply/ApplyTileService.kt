package io.github.barqallayl.burkan.feature.apply

import android.app.Service
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.core.storage.DeviceStateStorage
import io.github.barqallayl.burkan.feature.apply.data.ApplyController
import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.apply.data.ExternalTrigger
import io.github.barqallayl.burkan.feature.apply.data.TileStatus
import io.github.barqallayl.burkan.feature.apply.data.tileStatus
import io.github.barqallayl.burkan.feature.log.data.RunLogStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Quick Settings tile: whether Vulkan has been applied since the phone started, and one tap to apply it. A tap
 * always applies, which on a phone already on Vulkan finds nothing to do; going back to OpenGL is in the app, where
 * it is asked about first. A long press opens the app.
 */
@Inject
@ServiceKey
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
class ApplyTileService(
    private val controller: ApplyController,
    private val runLog: RunLogStorage,
    private val deviceState: DeviceStateStorage,
    private val trigger: ExternalTrigger,
    private val clock: Clock,
) : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        listening?.cancel()
        listening = scope.launch {
            combine(runLog.runs, controller.state, deviceState.isSetupComplete) { runs, run, setupComplete ->
                // When the phone started, by its own clock: now, less the time it has been up.
                val bootedAt = clock.now() - SystemClock.elapsedRealtime().milliseconds
                tileStatus(runs, run is ApplyRunState.Running, setupComplete, bootedAt)
            }.collect(::show)
        }
    }

    override fun onStopListening() {
        listening?.cancel()
    }

    override fun onClick() {
        scope.launch { trigger.onTile() }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun show(status: TileStatus) {
        val tile = qsTile ?: return
        tile.state = when (status) {
            TileStatus.Active -> Tile.STATE_ACTIVE
            TileStatus.Inactive -> Tile.STATE_INACTIVE
            TileStatus.Busy, TileStatus.Unavailable -> Tile.STATE_UNAVAILABLE
        }
        tile.label = getString(R.string.tile_label)
        tile.subtitle = getString(
            when (status) {
                TileStatus.Active -> R.string.tile_active
                TileStatus.Inactive -> R.string.tile_inactive
                TileStatus.Busy -> R.string.tile_busy
                TileStatus.Unavailable -> R.string.tile_unavailable
            },
        )
        tile.updateTile()
    }
}
