package io.github.barqallayl.burkan

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication
import io.github.barqallayl.burkan.core.di.AppGraph

class BurkanApplication : Application(), MetroApplication {

    private val appGraph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    override val appComponentProviders: MetroAppComponentProviders
        get() = appGraph
}
