package com.emanueledipietro.remodex.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.emanueledipietro.remodex.model.RemodexAppearanceMode
import com.emanueledipietro.remodex.model.RemodexAppFontStyle
import com.emanueledipietro.remodex.model.RemodexQueuedDraft
import com.emanueledipietro.remodex.model.RemodexRuntimeDefaults
import com.emanueledipietro.remodex.model.RemodexRuntimeOverrides
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val RemodexPreferencesName = "remodex_preferences"
private val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
private val SelectedThreadIdKey = stringPreferencesKey("selected_thread_id")
private val CollapsedProjectGroupIdsJsonKey = stringPreferencesKey("collapsed_project_group_ids_json")
private val DeletedThreadIdsJsonKey = stringPreferencesKey("deleted_thread_ids_json")
private val QueuedDraftsJsonKey = stringPreferencesKey("queued_drafts_json")
private val RuntimeOverridesJsonKey = stringPreferencesKey("runtime_overrides_json")
private val RuntimeDefaultsJsonKey = stringPreferencesKey("runtime_defaults_json")
private val AppearanceModeKey = stringPreferencesKey("appearance_mode")
private val AppFontStyleKey = stringPreferencesKey("app_font_style")
private val MacNicknamesJsonKey = stringPreferencesKey("mac_nicknames_json")
private val Context.remodexDataStore by preferencesDataStore(name = RemodexPreferencesName)

class DataStoreAppPreferencesRepository(
    private val context: Context,
) : AppPreferencesRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val preferences: Flow<AppPreferences> =
        context.remodexDataStore.data.map { preferences ->
            val queuedDrafts = preferences[QueuedDraftsJsonKey]
                ?.let { raw -> json.decodeFromString<QueuedDraftsEnvelope>(raw).threads }
                ?: emptyMap()
            val deletedThreadIds = preferences[DeletedThreadIdsJsonKey]
                ?.let { raw -> json.decodeFromString<DeletedThreadIdsEnvelope>(raw).threadIds }
                ?.toSet()
                ?: emptySet()
            val collapsedProjectGroupIds = preferences[CollapsedProjectGroupIdsJsonKey]
                ?.let { raw -> json.decodeFromString<CollapsedProjectGroupIdsEnvelope>(raw).groupIds }
                ?.toSet()
                ?: emptySet()
            val runtimeOverrides = preferences[RuntimeOverridesJsonKey]
                ?.let { raw -> json.decodeFromString<RuntimeOverridesEnvelope>(raw).threads }
                ?: emptyMap()
            val runtimeDefaults = preferences[RuntimeDefaultsJsonKey]
                ?.let { raw -> json.decodeFromString<RemodexRuntimeDefaults>(raw) }
                ?: RemodexRuntimeDefaults()
            val macNicknames = preferences[MacNicknamesJsonKey]
                ?.let { raw -> json.decodeFromString<MacNicknamesEnvelope>(raw).nicknames }
                ?: emptyMap()
            AppPreferences(
                onboardingCompleted = preferences[OnboardingCompletedKey] ?: false,
                selectedThreadId = preferences[SelectedThreadIdKey],
                collapsedProjectGroupIds = collapsedProjectGroupIds,
                deletedThreadIds = deletedThreadIds,
                queuedDraftsByThread = queuedDrafts,
                runtimeOverridesByThread = runtimeOverrides,
                runtimeDefaults = runtimeDefaults,
                appearanceMode = preferences[AppearanceModeKey]
                    ?.let(RemodexAppearanceMode::valueOf)
                    ?: RemodexAppearanceMode.SYSTEM,
                appFontStyle = preferences[AppFontStyleKey]
                    ?.let(RemodexAppFontStyle::valueOf)
                    ?: RemodexAppFontStyle.SYSTEM,
                macNicknamesByDeviceId = macNicknames,
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            preferences[OnboardingCompletedKey] = completed
        }
    }

    override suspend fun setSelectedThreadId(threadId: String?) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            if (threadId.isNullOrBlank()) {
                preferences.remove(SelectedThreadIdKey)
            } else {
                preferences[SelectedThreadIdKey] = threadId
            }
        }
    }

    override suspend fun setProjectGroupCollapsed(
        groupId: String,
        collapsed: Boolean,
    ) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            val normalizedGroupId = groupId.trim()
            if (normalizedGroupId.isEmpty()) {
                return@edit
            }
            val current = preferences[CollapsedProjectGroupIdsJsonKey]
                ?.let { raw -> json.decodeFromString<CollapsedProjectGroupIdsEnvelope>(raw).groupIds }
                ?.toMutableSet()
                ?: mutableSetOf()
            if (collapsed) {
                current.add(normalizedGroupId)
            } else {
                current.remove(normalizedGroupId)
            }
            preferences[CollapsedProjectGroupIdsJsonKey] = json.encodeToString(
                CollapsedProjectGroupIdsEnvelope(current.toList().sorted()),
            )
        }
    }

    override suspend fun setThreadDeleted(
        threadId: String,
        deleted: Boolean,
    ) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            val normalizedThreadId = threadId.trim()
            if (normalizedThreadId.isEmpty()) {
                return@edit
            }
            val current = preferences[DeletedThreadIdsJsonKey]
                ?.let { raw -> json.decodeFromString<DeletedThreadIdsEnvelope>(raw).threadIds }
                ?.toMutableSet()
                ?: mutableSetOf()
            if (deleted) {
                current.add(normalizedThreadId)
            } else {
                current.remove(normalizedThreadId)
            }
            preferences[DeletedThreadIdsJsonKey] = json.encodeToString(
                DeletedThreadIdsEnvelope(current.toList().sorted()),
            )
        }
    }

    override suspend fun setQueuedDrafts(
        threadId: String,
        drafts: List<RemodexQueuedDraft>,
    ) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            val current = preferences[QueuedDraftsJsonKey]
                ?.let { raw -> json.decodeFromString<QueuedDraftsEnvelope>(raw).threads }
                ?.toMutableMap()
                ?: mutableMapOf()
            if (drafts.isEmpty()) {
                current.remove(threadId)
            } else {
                current[threadId] = drafts
            }
            preferences[QueuedDraftsJsonKey] = json.encodeToString(QueuedDraftsEnvelope(current))
        }
    }

    override suspend fun setRuntimeOverrides(
        threadId: String,
        overrides: RemodexRuntimeOverrides?,
    ) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            val current = preferences[RuntimeOverridesJsonKey]
                ?.let { raw -> json.decodeFromString<RuntimeOverridesEnvelope>(raw).threads }
                ?.toMutableMap()
                ?: mutableMapOf()
            if (overrides == null) {
                current.remove(threadId)
            } else {
                current[threadId] = overrides
            }
            preferences[RuntimeOverridesJsonKey] = json.encodeToString(RuntimeOverridesEnvelope(current))
        }
    }

    override suspend fun setRuntimeDefaults(defaults: RemodexRuntimeDefaults) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            preferences[RuntimeDefaultsJsonKey] = json.encodeToString(defaults)
        }
    }

    override suspend fun setAppearanceMode(mode: RemodexAppearanceMode) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            preferences[AppearanceModeKey] = mode.name
        }
    }

    override suspend fun setAppFontStyle(style: RemodexAppFontStyle) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            preferences[AppFontStyleKey] = style.name
        }
    }

    override suspend fun setMacNickname(
        deviceId: String,
        nickname: String?,
    ) {
        context.remodexDataStore.edit { preferences: MutablePreferences ->
            val current = preferences[MacNicknamesJsonKey]
                ?.let { raw -> json.decodeFromString<MacNicknamesEnvelope>(raw).nicknames }
                ?.toMutableMap()
                ?: mutableMapOf()
            val normalizedDeviceId = deviceId.trim()
            if (normalizedDeviceId.isEmpty()) {
                return@edit
            }
            val normalizedNickname = nickname?.trim().orEmpty()
            if (normalizedNickname.isEmpty()) {
                current.remove(normalizedDeviceId)
            } else {
                current[normalizedDeviceId] = normalizedNickname
            }
            preferences[MacNicknamesJsonKey] = json.encodeToString(MacNicknamesEnvelope(current))
        }
    }
}

@Serializable
private data class QueuedDraftsEnvelope(
    val threads: Map<String, List<RemodexQueuedDraft>> = emptyMap(),
)

@Serializable
private data class DeletedThreadIdsEnvelope(
    val threadIds: List<String> = emptyList(),
)

@Serializable
private data class CollapsedProjectGroupIdsEnvelope(
    val groupIds: List<String> = emptyList(),
)

@Serializable
private data class RuntimeOverridesEnvelope(
    val threads: Map<String, RemodexRuntimeOverrides> = emptyMap(),
)

@Serializable
private data class MacNicknamesEnvelope(
    val nicknames: Map<String, String> = emptyMap(),
)
