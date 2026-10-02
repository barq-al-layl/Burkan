package io.github.barqallayl.burkan.feature.log.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import co.touchlab.kermit.Logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.barqallayl.burkan.core.model.AppErrorType
import io.github.barqallayl.burkan.feature.apply.model.ApplyKind
import io.github.barqallayl.burkan.feature.apply.model.RestoredSetting
import io.github.barqallayl.burkan.feature.apply.model.RunTrigger
import io.github.barqallayl.burkan.feature.apply.model.StepKind
import io.github.barqallayl.burkan.feature.log.model.LoggedStep
import io.github.barqallayl.burkan.feature.log.model.RunLogEntry
import io.github.barqallayl.burkan.feature.log.model.RunResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** The last [RunLogStorage.CAPACITY] runs, newest first. */
interface RunLogStorage {
    val runs: Flow<List<RunLogEntry>>

    /** True while the stored log could not be read: [runs] is then empty, though runs were recorded. */
    val unreadable: Flow<Boolean>

    suspend fun add(entry: RunLogEntry)

    companion object {
        const val CAPACITY = 50
    }
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DataStoreRunLogStorage(private val dataStore: DataStore<Preferences>) : RunLogStorage {

    private val read: Flow<Read> = dataStore.data
        .map { preferences -> decode(preferences[KEY]) }
        .catch { error -> if (error is IOException) emit(Read(emptyList(), unreadable = true)) else throw error }

    override val runs: Flow<List<RunLogEntry>> = read.map { it.runs }.distinctUntilChanged()

    override val unreadable: Flow<Boolean> = read.map { it.unreadable }.distinctUntilChanged()

    override suspend fun add(entry: RunLogEntry) {
        dataStore.edit { preferences ->
            val runs = listOf(entry.toStored()) + storedRuns(preferences[KEY])
            preferences[KEY] = json.encodeToString(runs.take(RunLogStorage.CAPACITY))
        }
    }

    private fun decode(text: String?): Read = try {
        Read(stored(text).mapNotNull { it.toEntry() }, unreadable = false)
    } catch (e: SerializationException) {
        Logger.w(e) { "Run log unreadable" }
        Read(emptyList(), unreadable = true)
    }

    /** A log this version cannot read is started afresh when the next run is added, rather than lost every time. */
    private fun storedRuns(text: String?): List<StoredRun> = try {
        stored(text)
    } catch (_: SerializationException) {
        emptyList()
    }

    private fun stored(text: String?): List<StoredRun> =
        if (text == null) emptyList() else json.decodeFromString<List<StoredRun>>(text)

    private data class Read(val runs: List<RunLogEntry>, val unreadable: Boolean)


    private companion object {
        val KEY = stringPreferencesKey("run_log")
        val json = Json { ignoreUnknownKeys = true }
    }
}

/** The stored shape. Enums are stored by name; a run naming something this version lacks is skipped. */
@Serializable
private data class StoredRun(
    val startedAtMillis: Long,
    val trigger: String,
    val kind: String,
    val result: String,
    val durationMillis: Long,
    val steps: List<StoredStep>,
    val error: String? = null,
)

@Serializable
private data class StoredStep(
    val kind: String,
    val count: Int? = null,
    val setting: String? = null,
    val error: String? = null,
)

private fun RunLogEntry.toStored() = StoredRun(
    startedAtMillis = startedAt.toEpochMilliseconds(),
    trigger = trigger.name,
    kind = kind.name,
    result = result.name,
    durationMillis = duration.inWholeMilliseconds,
    steps = steps.map { step ->
        val kind = step.kind
        StoredStep(
            kind = kind.storedName,
            count = (kind as? StepKind.StopApps)?.count ?: (kind as? StepKind.RelaunchApps)?.count,
            setting = (kind as? StepKind.RestoreSetting)?.setting?.name,
            error = step.error?.name,
        )
    },
    error = error?.name,
)

private fun StoredRun.toEntry(): RunLogEntry? {
    return RunLogEntry(
        startedAt = Instant.fromEpochMilliseconds(startedAtMillis),
        trigger = enumOrNull<RunTrigger>(trigger) ?: return null,
        kind = enumOrNull<ApplyKind>(kind) ?: return null,
        result = enumOrNull<RunResult>(result) ?: return null,
        duration = durationMillis.milliseconds,
        steps = steps.map { step ->
            LoggedStep(
                kind = step.toKind() ?: return null,
                error = step.error?.let(::errorType),
            )
        },
        error = error?.let(::errorType),
    )
}

private val StepKind.storedName: String
    get() = when (this) {
        StepKind.SetRenderer -> "SetRenderer"
        is StepKind.StopApps -> "StopApps"
        StepKind.RestartSystemUi -> "RestartSystemUi"
        StepKind.RestartLauncher -> "RestartLauncher"
        is StepKind.RelaunchApps -> "RelaunchApps"
        StepKind.RestartKeyboard -> "RestartKeyboard"
        is StepKind.RestoreSetting -> "RestoreSetting"
    }

private fun StoredStep.toKind(): StepKind? = when (kind) {
    "SetRenderer" -> StepKind.SetRenderer
    "StopApps" -> count?.let(StepKind::StopApps)
    "RestartSystemUi" -> StepKind.RestartSystemUi
    "RestartLauncher" -> StepKind.RestartLauncher
    "RelaunchApps" -> count?.let(StepKind::RelaunchApps)
    "RestartKeyboard" -> StepKind.RestartKeyboard
    "RestoreSetting" -> setting?.let { enumOrNull<RestoredSetting>(it) }?.let(StepKind::RestoreSetting)
    else -> null
}

/** An error this version no longer has a message for still reads as a failure. */
private fun errorType(name: String): AppErrorType = enumOrNull<AppErrorType>(name) ?: AppErrorType.Unexpected

private inline fun <reified E : Enum<E>> enumOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
