package io.github.barqallayl.burkan.core.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@ContributesTo(AppScope::class)
@BindingContainer
object StorageBindings {

    /** One instance per file for the life of the process: DataStore fails if a second one opens the same file. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(application: Application): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { application.preferencesDataStoreFile("settings") }
}
