package io.github.barqallayl.burkan.core.di

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings

@DependencyGraph(AppScope::class)
interface AppGraph : MetroAppComponentProviders, MetroViewModelMultibindings {

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
