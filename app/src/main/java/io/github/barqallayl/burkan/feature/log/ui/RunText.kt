package io.github.barqallayl.burkan.feature.log.ui

import androidx.annotation.StringRes
import io.github.barqallayl.burkan.R
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.log.model.RunResult

/** Names for the parts of a run, shared by Home, the Log and the exported log. */
@get:StringRes
val ApplyKind.label: Int
    get() = when (this) {
        ApplyKind.Light -> R.string.kind_light
        ApplyKind.Full -> R.string.kind_full
    }

@get:StringRes
val RunTrigger.label: Int
    get() = when (this) {
        RunTrigger.Manual -> R.string.trigger_manual
        RunTrigger.Boot -> R.string.trigger_boot
    }

@get:StringRes
val RunResult.label: Int
    get() = when (this) {
        RunResult.Succeeded -> R.string.result_succeeded
        RunResult.Failed -> R.string.result_failed
        RunResult.Cancelled -> R.string.result_cancelled
    }
