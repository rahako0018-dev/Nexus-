package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.GamingProfileType
import com.example.model.SidebarConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nexus_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val POSITION_LEFT = booleanPreferencesKey("pos_left")
        val VERTICAL_PERCENT = floatPreferencesKey("vert_pct")
        val HANDLE_OPACITY = floatPreferencesKey("opacity")
        val HANDLE_SIZE = floatPreferencesKey("size_mult")
        val ACCENT_COLOR = longPreferencesKey("accent_hex")
        val COMPACT_MODE = booleanPreferencesKey("compact")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic")
        val PROFILE_TYPE = stringPreferencesKey("profile_type")
        val QUICK_NOTES = stringPreferencesKey("quick_notes")
        val SELECTED_GAME_PKG = stringPreferencesKey("selected_game_pkg")
        val LAST_SESSION_ACTIVE = booleanPreferencesKey("last_session_active")
    }

    val configFlow: Flow<SidebarConfig> = context.dataStore.data.map { prefs ->
        val profileName = prefs[Keys.PROFILE_TYPE] ?: GamingProfileType.BALANCED.name
        val profile = try {
            GamingProfileType.valueOf(profileName)
        } catch (_: Exception) {
            GamingProfileType.BALANCED
        }

        SidebarConfig(
            isPositionLeft = prefs[Keys.POSITION_LEFT] ?: false,
            verticalPercent = prefs[Keys.VERTICAL_PERCENT] ?: 0.45f,
            handleOpacity = prefs[Keys.HANDLE_OPACITY] ?: 0.85f,
            handleSizeMultiplier = prefs[Keys.HANDLE_SIZE] ?: 1.0f,
            accentColorHex = prefs[Keys.ACCENT_COLOR] ?: 0xFF00E5FF,
            isCompactMode = prefs[Keys.COMPACT_MODE] ?: false,
            enableSoundOrHaptic = prefs[Keys.HAPTIC_ENABLED] ?: true,
            activeProfileType = profile,
            quickNotes = prefs[Keys.QUICK_NOTES] ?: ""
        )
    }

    val selectedGameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SELECTED_GAME_PKG]
    }

    val lastSessionActiveFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SESSION_ACTIVE] ?: false
    }

    suspend fun updatePosition(isLeft: Boolean, verticalPercent: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.POSITION_LEFT] = isLeft
            prefs[Keys.VERTICAL_PERCENT] = verticalPercent.coerceIn(0.05f, 0.95f)
        }
    }

    suspend fun updateAppearance(opacity: Float, sizeMultiplier: Float, compact: Boolean, accentColor: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HANDLE_OPACITY] = opacity.coerceIn(0.2f, 1.0f)
            prefs[Keys.HANDLE_SIZE] = sizeMultiplier.coerceIn(0.7f, 1.5f)
            prefs[Keys.COMPACT_MODE] = compact
            prefs[Keys.ACCENT_COLOR] = accentColor
        }
    }

    suspend fun updateProfile(profileType: GamingProfileType) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_TYPE] = profileType.name
        }
    }

    suspend fun updateQuickNotes(notes: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.QUICK_NOTES] = notes
        }
    }

    suspend fun setSelectedGame(packageName: String?) {
        context.dataStore.edit { prefs ->
            if (packageName != null) {
                prefs[Keys.SELECTED_GAME_PKG] = packageName
            } else {
                prefs.remove(Keys.SELECTED_GAME_PKG)
            }
        }
    }

    suspend fun setLastSessionActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_SESSION_ACTIVE] = active
        }
    }
}
