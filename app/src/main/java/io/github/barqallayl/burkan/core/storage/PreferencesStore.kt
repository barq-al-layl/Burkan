package io.github.barqallayl.burkan.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.File
import java.io.IOException

/**
 * What the store holds, or no preferences at all when the file cannot be read: the app goes on as after a fresh
 * install rather than going down with it.
 */
val DataStore<Preferences>.dataOrEmpty: Flow<Preferences>
    get() = data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

/**
 * Writes to the store, and when the phone cannot write, storage being full for one, says so in the log and goes on.
 * What is kept here is the app's memory of what it did: losing a line of it is better than a run that throws while
 * it is putting the phone's settings back.
 *
 * True when it was written. A caller that must not go on without the write, as a full apply must not change the
 * phone's settings before they are kept, checks it.
 */
suspend fun DataStore<Preferences>.editOrLog(transform: suspend (MutablePreferences) -> Unit): Boolean = try {
    edit(transform)
    true
} catch (e: IOException) {
    Logger.w(e) { "A write to the app's preferences failed" }
    false
}

/**
 * A preferences store kept in the file [produceFile] names.
 *
 * A file that can no longer be read as preferences is replaced by an empty one, so the app starts afresh. Left as it
 * was, it would fail every read, and every write too: the run log, the boot count and each change in Settings.
 */
fun preferencesStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
    produceFile = produceFile,
)
