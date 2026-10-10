package io.github.barqallayl.burkan.core.di

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.storage.DataStoreDeviceStateStorage
import io.github.barqallayl.burkan.core.storage.preferencesStore

@ContributesTo(AppScope::class)
@BindingContainer
object StorageBindings {

    /** One instance per file for the life of the process: DataStore fails if a second one opens the same file. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideSettingsDataStore(application: Application): DataStore<Preferences> =
        preferencesStore { application.preferencesDataStoreFile("settings") }

    /** A separate file, so the backup rules can leave it out. */
    @Provides
    @SingleIn(AppScope::class)
    @Named(DataStoreDeviceStateStorage.DEVICE_STATE_STORE)
    fun provideDeviceStateDataStore(application: Application): DataStore<Preferences> =
        preferencesStore { application.preferencesDataStoreFile(DataStoreDeviceStateStorage.DEVICE_STATE_STORE) }
}
