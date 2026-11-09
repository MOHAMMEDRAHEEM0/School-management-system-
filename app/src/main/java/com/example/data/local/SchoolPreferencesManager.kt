package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "school_preferences")

/**
 * Production DataStore Preference Manager.
 * Solves the first-time verification bug by persisting:
 * 1. isSchoolVerified: Strictly checked once during initial onboarding.
 * 2. isSetupCompleted: Marks that the initial institution and master profile setup has run.
 * 3. verifiedSchoolCode & verifiedInstitutionId: Bound to the active tenant.
 * 4. savedUserRole & savedUserId: Enables direct routing to Authenticated Dashboard on app relaunch.
 */
@Singleton
class SchoolPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_IS_SCHOOL_VERIFIED = booleanPreferencesKey("is_school_verified")
        val KEY_IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val KEY_VERIFIED_SCHOOL_CODE = stringPreferencesKey("verified_school_code")
        val KEY_VERIFIED_INSTITUTION_ID = stringPreferencesKey("verified_institution_id")
        val KEY_SAVED_USER_ID = stringPreferencesKey("saved_user_id")
        val KEY_SAVED_USER_ROLE = stringPreferencesKey("saved_user_role")
        val KEY_SAVED_DISPLAY_NAME = stringPreferencesKey("saved_display_name")
        val KEY_SAVED_SESSION_TOKEN = stringPreferencesKey("saved_session_token")
    }

    private val dataStore = context.dataStore

    val isSchoolVerifiedFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_IS_SCHOOL_VERIFIED] ?: false
        }

    val isSetupCompletedFlow: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_IS_SETUP_COMPLETED] ?: false
        }

    val verifiedSchoolCodeFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_VERIFIED_SCHOOL_CODE] ?: "3477"
        }

    val verifiedInstitutionIdFlow: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_VERIFIED_INSTITUTION_ID] ?: "INST-SXA-2026"
        }

    val savedUserRoleFlow: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[KEY_SAVED_USER_ROLE]
        }

    suspend fun isSchoolVerifiedOnce(): Boolean = try {
        isSchoolVerifiedFlow.first()
    } catch (_: Exception) {
        false
    }

    suspend fun isSetupCompletedOnce(): Boolean = try {
        isSetupCompletedFlow.first()
    } catch (_: Exception) {
        false
    }

    suspend fun getVerifiedSchoolCode(): String = try {
        verifiedSchoolCodeFlow.first()
    } catch (_: Exception) {
        "3477"
    }

    suspend fun getVerifiedInstitutionId(): String = try {
        verifiedInstitutionIdFlow.first()
    } catch (_: Exception) {
        "INST-SXA-2026"
    }

    suspend fun getSavedUserRole(): String? = try {
        savedUserRoleFlow.first()
    } catch (_: Exception) {
        null
    }

    suspend fun setSchoolVerified(
        verified: Boolean,
        schoolCode: String,
        institutionId: String
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_SCHOOL_VERIFIED] = verified
            preferences[KEY_VERIFIED_SCHOOL_CODE] = schoolCode
            preferences[KEY_VERIFIED_INSTITUTION_ID] = institutionId
            if (verified) {
                preferences[KEY_IS_SETUP_COMPLETED] = true
            }
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_IS_SETUP_COMPLETED] = completed
        }
    }

    suspend fun saveAuthenticatedSession(
        userId: String,
        role: String,
        displayName: String,
        token: String
    ) {
        dataStore.edit { preferences ->
            preferences[KEY_SAVED_USER_ID] = userId
            preferences[KEY_SAVED_USER_ROLE] = role
            preferences[KEY_SAVED_DISPLAY_NAME] = displayName
            preferences[KEY_SAVED_SESSION_TOKEN] = token
            preferences[KEY_IS_SCHOOL_VERIFIED] = true
            preferences[KEY_IS_SETUP_COMPLETED] = true
        }
    }

    suspend fun clearSessionOnSignOut() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_SAVED_USER_ID)
            preferences.remove(KEY_SAVED_USER_ROLE)
            preferences.remove(KEY_SAVED_DISPLAY_NAME)
            preferences.remove(KEY_SAVED_SESSION_TOKEN)
            // School verification is intentionally kept so user doesn't have to re-enter registration code on re-login
        }
    }

    suspend fun resetAllOnboarding() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
