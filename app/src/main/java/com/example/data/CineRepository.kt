package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.AppConfigEntity
import com.example.data.local.VipUserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException

// Extension property to obtain global Datastore instance safely
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cine_premium_prefs")

class CineRepository(private val context: Context) {

    companion object {
        private val IS_VIP_KEY = booleanPreferencesKey("is_user_vip")
        private val USER_ID_KEY = stringPreferencesKey("user_profile_id")
        
        const val CONFIG_KEY_AGGREGATOR_URL = "aggregator_base_url"
        const val CONFIG_KEY_TMDB_API_KEY = "tmdb_api_key"
        const val DEFAULT_AGGREGATOR_URL = "https://embed.warezcdn.com"
        const val DEFAULT_TMDB_API_KEY = "" // Empty by default, allows user to input liveTMDB Key in Admin panel
    }

    // Lazy Room Database Initialization
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "cine_premium_admin_db"
        ).fallbackToDestructiveMigration().build()
    }

    // Flow that emits changes to the VIP premium state
    val isUserVipFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_VIP_KEY] ?: false // Default to false (Not VIP)
        }

    // Unique user profile id (cached in DataStore)
    val userProfileIdFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            var currentId = preferences[USER_ID_KEY]
            if (currentId.isNullOrEmpty()) {
                // If nonexistent, generate and let next edit update it
                val randomId = "CINE-${(1000..9999).random()}"
                currentId = randomId
            }
            currentId
        }

    // Initialize unique ID if missing
    suspend fun getOrInitializeUserId(): String {
        var initializedId = ""
        context.dataStore.edit { preferences ->
            val curId = preferences[USER_ID_KEY]
            if (curId.isNullOrEmpty()) {
                val newId = "CINE-${(1000..9999).random()}"
                preferences[USER_ID_KEY] = newId
                initializedId = newId
            } else {
                initializedId = curId
            }
        }
        return initializedId
    }

    // Function to set VIP premium status locally
    suspend fun setVipStatus(isVip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_VIP_KEY] = isVip
        }
        
        // Also reflect it in global Room entity database cache for consistency with profile
        val profileId = getOrInitializeUserId()
        val userItem = db.adminDao.getVipUserById(profileId)
        if (userItem != null) {
            db.adminDao.insertVipUser(userItem.copy(isVip = isVip))
        } else {
            db.adminDao.insertVipUser(VipUserEntity(profileId, "Você (Dispositivo Local)", isVip))
        }
    }

    // --- ROOM DATABASE FOR SECRET REGISTRATION CONTROL --

    val allVipRequestsFlow: Flow<List<VipUserEntity>> = db.adminDao.getAllVipUsersFlow()

    suspend fun insertVipUserRequest(userId: String, name: String, isVip: Boolean) {
        db.adminDao.insertVipUser(VipUserEntity(id = userId, name = name, isVip = isVip))
    }

    suspend fun setCustomUserVipStatus(userId: String, isVip: Boolean) {
        val user = db.adminDao.getVipUserById(userId)
        if (user != null) {
            db.adminDao.insertVipUser(user.copy(isVip = isVip))
        } else {
            db.adminDao.insertVipUser(VipUserEntity(userId, "Cliente VIP", isVip))
        }
        
        // If the updated user is the current local device, update the local dataStore too!
        val myId = getOrInitializeUserId()
        if (userId == myId) {
            context.dataStore.edit { prefs ->
                prefs[IS_VIP_KEY] = isVip
            }
        }
    }

    // Sync current user status with VIP database (called during app startup or profile page view)
    suspend fun syncLocalVipStatusFromDatabase() {
        val myId = getOrInitializeUserId()
        val userEntity = db.adminDao.getVipUserById(myId)
        if (userEntity != null) {
            context.dataStore.edit { prefs ->
                prefs[IS_VIP_KEY] = userEntity.isVip
            }
        }
    }

    // --- CONFIG READS/WRITES --

    suspend fun getAppConfig(key: String, defaultValue: String): String {
        return db.adminDao.getConfigByKey(key)?.configValue ?: defaultValue
    }

    suspend fun setAppConfig(key: String, value: String) {
        db.adminDao.insertConfig(AppConfigEntity(key, value))
    }

    /**
     * Scrapes a third-party server website to extract an .m3u8 video URL inside an iframe.
     * Uses Dispatchers.IO for safe network operations.
     */
    suspend fun scrapeM3u8Url(pageUrl: String): String = withContext(Dispatchers.IO) {
        try {
            // Setup scraper connection with proper user agent representing a web browser
            val connection = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000) // 10s timeout protective check
                .followRedirects(true)

            val document = connection.get()
            
            // Step 1: Search for iframes containing source player urls
            val iframes = document.select("iframe")
            for (iframe in iframes) {
                val iframeSrc = iframe.attr("src")
                if (iframeSrc.isNotEmpty()) {
                    // Sometimes the clean .m3u8 belongs directly in the main query params
                    val m3u8Match = findM3u8Pattern(iframeSrc)
                    if (m3u8Match != null) return@withContext m3u8Match

                    // Step 2: Recursively scrape the nested iframe HTML document for video elements or javascript sources
                    try {
                        val nestedDoc = Jsoup.connect(iframeSrc)
                            .userAgent("Mozilla/5.0 (Windows / Android Device)")
                            .timeout(5000)
                            .ignoreContentType(true)
                            .get()

                        val scripts = nestedDoc.select("script")
                        for (script in scripts) {
                            val scriptContent = script.html()
                            val match = findM3u8Pattern(scriptContent)
                            if (match != null) return@withContext match
                        }

                        // Search normal sources in media tags
                        val source = nestedDoc.select("source")
                        for (srcElement in source) {
                            val srcUrl = srcElement.attr("src")
                            if (srcUrl.contains(".m3u8")) {
                                return@withContext srcUrl
                            }
                        }
                    } catch (e: Exception) {
                        // Suppress nested exceptions to proceed parsing alternative components
                    }
                }
            }

            // Step 3: Scan the primary source script tags as fallback
            val scripts = document.select("script")
            for (script in scripts) {
                val scriptContent = script.html()
                val match = findM3u8Pattern(scriptContent)
                if (match != null) return@withContext match
            }

            // Fallback: search raw HTML string for any occurrences of .m3u8 URLs
            val rawHtml = document.html()
            val finalMatch = findM3u8Pattern(rawHtml)
            if (finalMatch != null) return@withContext finalMatch

            throw Exception("Nenhum arquivo de streaming .m3u8 foi encontrado no link informado.")
        } catch (ioe: IOException) {
            throw Exception("Falha de conexão ou timeout ao acessar o servidor remoto.")
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Uses regex expression patterns to find clean .m3u8 URL routes inside dynamic containers.
     */
    private fun findM3u8Pattern(text: String): String? {
        val regex = Regex("""https?://[^\s"'<>]+?\.m3u8[^\s"'<>]*""")
        val matchResult = regex.find(text)
        return matchResult?.value
    }
}
