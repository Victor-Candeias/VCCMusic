package pt.vcc.vccmusic.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themePreferencesDataStore by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    fun observeDarkTheme(context: Context): Flow<Boolean> =
        context.themePreferencesDataStore.data.map { preferences ->
            preferences[darkThemeKey] ?: false
        }

    suspend fun setDarkTheme(context: Context, enabled: Boolean) {
        context.themePreferencesDataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }
}
