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
import com.google.firebase.firestore.FirebaseFirestore
// KTX imports removed
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume

// Extension property to obtain global Datastore instance safely
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cine_premium_prefs")

class CineRepository(private val context: Context) {

    companion object {
        private val IS_VIP_KEY = booleanPreferencesKey("is_user_vip")
        private val USER_ID_KEY = stringPreferencesKey("user_profile_id")
        
        const val CONFIG_KEY_AGGREGATOR_URL = "aggregator_base_url"
        const val CONFIG_KEY_TMDB_API_KEY = "tmdb_api_key"
        const val DEFAULT_AGGREGATOR_URL = "https://megacine.boats"
        const val DEFAULT_TMDB_API_KEY = "c746904c8c640ee387f3d28f3d451c0e"
    }

    // Instância do Firebase Firestore
    private val firestore: FirebaseFirestore by lazy {
        try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        FirebaseFirestore.getInstance()
    }

    // Flow that emits changes to the VIP premium state
    val isUserVipFlow: Flow<Boolean> = callbackFlow {
        val subscription = firestore.collection("vip_users").document(getOrInitializeUserId())
            .addSnapshotListener { snapshot, _ ->
                val isVip = snapshot?.getBoolean("isVip") ?: false
                trySend(isVip)
            }
        awaitClose { subscription.remove() }
    }

    val customMoviesFlow: Flow<List<com.example.ui.screens.Movie>> = callbackFlow {
        val subscription = firestore.collection("movies").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getLong("id")?.toInt() ?: 0
                    val title = doc.getString("title") ?: ""
                    val year = doc.getString("year") ?: ""
                    val category = doc.getString("category") ?: ""
                    val type = doc.getString("type") ?: "Filme"
                    val section = doc.getString("section") ?: "Lançamentos"
                    val rating = doc.getString("rating") ?: ""
                    val isVIP = doc.getBoolean("isVIP") ?: false
                    val duration = doc.getString("duration") ?: ""
                    val description = doc.getString("description") ?: ""
                    val posterUrl = doc.getString("posterUrl") ?: ""
                    val bannerUrl = doc.getString("bannerUrl") ?: ""
                    val videoUrl = doc.getString("videoUrl") ?: ""
                    val pageUrlToScrape = doc.getString("pageUrlToScrape") ?: ""
                    
                    com.example.ui.screens.Movie(
                        id = id, title = title, year = year, category = category,
                        type = type, section = section, rating = rating,
                        isVIP = isVIP, duration = duration, description = description,
                        accentTone = androidx.compose.ui.graphics.Color(0xFFE50914), // CineRed fallback
                        posterUrl = posterUrl, bannerUrl = bannerUrl, videoUrl = videoUrl,
                        pageUrlToScrape = pageUrlToScrape
                    )
                }
                trySend(list)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addCustomMovie(movie: com.example.ui.screens.Movie) {
        val map = mapOf(
            "id" to movie.id, "title" to movie.title, "year" to movie.year, "category" to movie.category,
            "type" to movie.type, "section" to movie.section,
            "rating" to movie.rating, "isVIP" to movie.isVIP, "duration" to movie.duration,
            "description" to movie.description, "posterUrl" to movie.posterUrl, "bannerUrl" to movie.bannerUrl,
            "videoUrl" to movie.videoUrl, "pageUrlToScrape" to movie.pageUrlToScrape
        )
        firestore.collection("movies").document(movie.id.toString()).set(map).await()
    }

    suspend fun deleteCustomMovie(movieId: Int) {
        firestore.collection("movies").document(movieId.toString()).delete().await()
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
        
        val profileId = getOrInitializeUserId()
        try {
            val doc = firestore.collection("vip_users").document(profileId).get().await()
            if (doc.exists()) {
                firestore.collection("vip_users").document(profileId).update("isVip", isVip).await()
            } else {
                firestore.collection("vip_users").document(profileId).set(
                    VipUserEntity(profileId, "Você (Dispositivo Local)", isVip)
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FIREBASE FIRESTORE FOR REALTIME CONTROL --

    val allVipRequestsFlow: Flow<List<VipUserEntity>> = callbackFlow {
        val listener = firestore.collection("vip_users")
            .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(VipUserEntity::class.java)
                    }
                    trySend(users)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun insertVipUserRequest(userId: String, name: String, isVip: Boolean) {
        firestore.collection("vip_users").document(userId).set(
            VipUserEntity(id = userId, name = name, isVip = isVip)
        ).await()
    }

    suspend fun setCustomUserVipStatus(userId: String, isVip: Boolean) {
        try {
            val doc = firestore.collection("vip_users").document(userId).get().await()
            if (doc.exists()) {
                firestore.collection("vip_users").document(userId).update("isVip", isVip).await()
            } else {
                firestore.collection("vip_users").document(userId).set(
                    VipUserEntity(userId, "Cliente VIP", isVip)
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val myId = getOrInitializeUserId()
        if (userId == myId) {
            context.dataStore.edit { prefs ->
                prefs[IS_VIP_KEY] = isVip
            }
        }
    }

    suspend fun deleteVipUser(userId: String) {
        firestore.collection("vip_users").document(userId).delete().await()
        val myId = getOrInitializeUserId()
        if (userId == myId) {
            context.dataStore.edit { prefs ->
                prefs[IS_VIP_KEY] = false
            }
        }
    }

    suspend fun updateVipUser(user: VipUserEntity) {
        firestore.collection("vip_users").document(user.id).set(user).await()
        val myId = getOrInitializeUserId()
        if (user.id == myId) {
            context.dataStore.edit { prefs ->
                val isExpired = user.expirationTime > 0L && user.expirationTime < System.currentTimeMillis()
                prefs[IS_VIP_KEY] = user.isVip && !user.isBanned && !isExpired
            }
        }
    }

    // Sync current user status with Firebase database
    suspend fun syncLocalVipStatusFromDatabase() {
        val myId = getOrInitializeUserId()
        try {
            val doc = firestore.collection("vip_users").document(myId).get().await()
            if (doc.exists()) {
                val user = doc.toObject(VipUserEntity::class.java)
                if (user != null) {
                    context.dataStore.edit { prefs ->
                        val isExpired = user.expirationTime > 0L && user.expirationTime < System.currentTimeMillis()
                        prefs[IS_VIP_KEY] = user.isVip && !user.isBanned && !isExpired
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAppConfig(key: String, defaultValue: String): String {
        return try {
            val doc = firestore.collection("app_configs").document(key).get().await()
            if (doc.exists()) doc.getString("configValue") ?: defaultValue else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    suspend fun setAppConfig(key: String, value: String) {
        try {
            firestore.collection("app_configs").document(key).set(mapOf("configValue" to value)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun extractIframeFromPortal(pageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = org.jsoup.Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10000)
                .get()
            
            // Procura por iframes no conteúdo principal
            val iframes = doc.select("iframe")
            for (iframe in iframes) {
                val src = iframe.attr("src")
                if (src.contains("viewplayer") || src.contains("embed") || src.contains("player")) {
                    return@withContext src
                }
            }
            return@withContext null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scrapes a third-party server website to extract an .m3u8 video URL.
     * Substitui o Jsoup por um WebView Headless rodando na Main Thread para processar JavaScript e Cloudflare RUM.
     */
    suspend fun scrapeM3u8Url(pageUrl: String): String = withContext(Dispatchers.Main) {
        var webView: WebView? = null
        try {
            // Tempo limite de 25 segundos, pois Cloudflare pode levar até 10-15 seg para passar
            val m3u8Url = withTimeoutOrNull(25000L) {
                suspendCancellableCoroutine<String> { continuation ->
                    var isResumed = false
                    
                    webView = WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url?.toString() ?: ""
                                
                                // O app espiona todas as requisições, incluindo iframes do Cloudflare
                                // Se a URL requisitada for um vídeo m3u8, nós roubamos a URL e retornamos pro player!
                                if (url.contains(".m3u8") && !isResumed) {
                                    isResumed = true
                                    continuation.resume(url)
                                }
                                
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        
                        // Inicia o carregamento "invisível" que vai rodar os JS e bypassar bloqueios
                        loadUrl(pageUrl)
                    }
                    
                    continuation.invokeOnCancellation {
                        // Cleanup handled by finally block
                    }
                }
            }
            
            if (m3u8Url != null) {
                return@withContext m3u8Url
            } else {
                throw Exception("Tempo limite excedido. O Cloudflare bloqueou ou o vídeo não existe.")
            }
        } finally {
            webView?.destroy()
        }
    }
}
