package io.github.barqallayl.burkan.feature.status.ui

import io.github.barqallayl.burkan.feature.apply.data.ApplyRunState
import io.github.barqallayl.burkan.feature.status.model.Headline
import io.github.barqallayl.burkan.feature.status.model.headline

/** The answer the status card gives, once the status is read: null while it is read, and when it could not be. */
val HomeState.headline: Headline?
    get() = (status as? StatusState.Loaded)?.status?.headline(systemUiAtNextLock)

/**
 * What Home offers as its one main action, which follows what there is to do. Material docks it at the foot of the
 * screen; One UI puts it first in the summary card.
 */
sealed interface MainAction {
    /** Something is not on Vulkan: Apply now, as the action that matters. */
    data object Apply : MainAction

    /** Nothing is left to do, or what is left happens by itself: Apply now is still there, but quiet. */
    data object ApplyQuietly : MainAction

    /** Vulkan is active and [count] open apps are left behind: restarting them is what is left to do. */
    data class RestartBehind(val count: Int) : MainAction

    /** The phone cannot be asked, so nothing can be applied either: Apply now is there, but unavailable. */
    data object Unavailable : MainAction

    /** A run is under way, and the action stops it. */
    data object Cancel : MainAction
}

val HomeState.mainAction: MainAction
    get() = when {
        run is ApplyRunState.Running -> MainAction.Cancel
        status is StatusState.Failed -> MainAction.Unavailable
        else -> when (headline) {
            Headline.VulkanActive -> behind?.takeIf { it.isNotEmpty() }?.let { MainAction.RestartBehind(it.size) }
                ?: MainAction.ApplyQuietly
            // System UI switches at the next lock by itself; applying now would lock the phone in front of its owner.
            Headline.WaitingForLock -> MainAction.ApplyQuietly
            Headline.NotApplied, Headline.PartlyApplied, Headline.Unknown, null -> MainAction.Apply
        }
    }
