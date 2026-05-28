package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CineRepository
import com.example.data.LinkExtractor
import com.example.data.tmdb.TmdbClient
import com.example.data.tmdb.TmdbMovie
import com.example.ui.screens.Movie
import com.example.ui.screens.MovieMockData
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CineGold
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder

sealed interface ScrapeUiState {
    data object Idle : ScrapeUiState
    data object Loading : ScrapeUiState
    val isLoading: Boolean get() = this is Loading
    data class Success(val streamUrl: String) : ScrapeUiState
    data class Error(val errorMsg: String) : ScrapeUiState
}

class MovieViewModel(private val repository: CineRepository) : ViewModel() {

    private val linkExtractor = LinkExtractor()

    // Real-time custom settings & client status caches
    val isUserVip: StateFlow<Boolean> = repository.isUserVipFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _userProfileId = MutableStateFlow("CINE-XXXX")
    val userProfileId: StateFlow<String> = _userProfileId.asStateFlow()

    private val _vipUsersList = MutableStateFlow<List<com.example.data.local.VipUserEntity>>(emptyList())
    val vipUsersList: StateFlow<List<com.example.data.local.VipUserEntity>> = _vipUsersList.asStateFlow()

    private val _aggregatorUrl = MutableStateFlow(CineRepository.DEFAULT_AGGREGATOR_URL)
    val aggregatorUrl: StateFlow<String> = _aggregatorUrl.asStateFlow()

    private val _tmdbApiKey = MutableStateFlow(CineRepository.DEFAULT_TMDB_API_KEY)
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    // Dynamic carousels powered by TMDB
    private val _popularMovies = MutableStateFlow<List<Movie>>(emptyList())
    val popularMovies: StateFlow<List<Movie>> = _popularMovies.asStateFlow()

    private val _trendingMovies = MutableStateFlow<List<Movie>>(emptyList())
    val trendingMovies: StateFlow<List<Movie>> = _trendingMovies.asStateFlow()

    private val _topRatedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val topRatedMovies: StateFlow<List<Movie>> = _topRatedMovies.asStateFlow()

    private val _isCatalogLoading = MutableStateFlow(false)
    val isCatalogLoading: StateFlow<Boolean> = _isCatalogLoading.asStateFlow()

    private val _scrapeState = MutableStateFlow<ScrapeUiState>(ScrapeUiState.Idle)
    val scrapeState: StateFlow<ScrapeUiState> = _scrapeState.asStateFlow()

    private val _activePlayingMovie = MutableStateFlow<Movie?>(null)
    val activePlayingMovie: StateFlow<Movie?> = _activePlayingMovie.asStateFlow()

    init {
        // Initialize user unique ID and settings
        viewModelScope.launch {
            val uid = repository.getOrInitializeUserId()
            _userProfileId.value = uid
            
            // Sync with local Room table VIP list
            repository.syncLocalVipStatusFromDatabase()
            
            // Read active setup preferences
            _aggregatorUrl.value = repository.getAppConfig(CineRepository.CONFIG_KEY_AGGREGATOR_URL, CineRepository.DEFAULT_AGGREGATOR_URL)
            _tmdbApiKey.value = repository.getAppConfig(CineRepository.CONFIG_KEY_TMDB_API_KEY, CineRepository.DEFAULT_TMDB_API_KEY)
            
            loadTmdbCatalogs()
            observeVipUsers()
        }
    }

    private fun observeVipUsers() {
        viewModelScope.launch {
            repository.allVipRequestsFlow.collect { list ->
                _vipUsersList.value = list
            }
        }
    }

    fun loadTmdbCatalogs() {
        viewModelScope.launch {
            _isCatalogLoading.value = true
            val key = _tmdbApiKey.value.trim()
            if (key.isEmpty()) {
                // If no key, default gracefully to local pre-curated cinematic structures
                _popularMovies.value = MovieMockData.lancamentos
                _trendingMovies.value = MovieMockData.recomendados
                _topRatedMovies.value = MovieMockData.lancamentos.reversed()
                _isCatalogLoading.value = false
                return@launch
            }

            try {
                val popularResponse = TmdbClient.service.getPopularMovies(key)
                _popularMovies.value = popularResponse.results.map { it.toDomainMovie() }

                val trendingResponse = TmdbClient.service.getTrendingMovies(key)
                _trendingMovies.value = trendingResponse.results.map { it.toDomainMovie() }

                val topRatedResponse = TmdbClient.service.getTopRatedMovies(key)
                _topRatedMovies.value = topRatedResponse.results.map { it.toDomainMovie() }
            } catch (e: Exception) {
                Log.e("CinePremium", "TMDB API Error, defaulting to pre-curated fallback catalog", e)
                _popularMovies.value = MovieMockData.lancamentos
                _trendingMovies.value = MovieMockData.recomendados
                _topRatedMovies.value = MovieMockData.lancamentos.reversed()
            } finally {
                _isCatalogLoading.value = false
            }
        }
    }

    fun updateConfigs(newAggregatorUrl: String, newTmdbKey: String) {
        viewModelScope.launch {
            repository.setAppConfig(CineRepository.CONFIG_KEY_AGGREGATOR_URL, newAggregatorUrl)
            repository.setAppConfig(CineRepository.CONFIG_KEY_TMDB_API_KEY, newTmdbKey)
            _aggregatorUrl.value = newAggregatorUrl
            _tmdbApiKey.value = newTmdbKey
            
            // Reload metadata if key changed
            loadTmdbCatalogs()
        }
    }

    fun setVipStatus(isVip: Boolean) {
        viewModelScope.launch {
            repository.setVipStatus(isVip)
        }
    }

    fun setCustomUserVipStatus(userId: String, isVip: Boolean) {
        viewModelScope.launch {
            repository.setCustomUserVipStatus(userId, isVip)
        }
    }

    /**
     * Users call this to submit/register their user ID as someone requesting VIP,
     * which will show up on the admin panel lists.
     */
    fun registerVipRequested(userId: String, name: String) {
        viewModelScope.launch {
            repository.insertVipUserRequest(userId, name, isVip = false)
        }
    }

    fun deleteVipUser(userId: String) {
        viewModelScope.launch {
            repository.deleteVipUser(userId)
        }
    }

    fun updateVipUser(user: com.example.data.local.VipUserEntity) {
        viewModelScope.launch {
            repository.updateVipUser(user)
        }
    }

    fun setActiveMovie(movie: Movie?) {
        _activePlayingMovie.value = movie
        if (movie == null) {
            _scrapeState.value = ScrapeUiState.Idle
        }
    }

    /**
     * Jsoup scraper utilizing TMDB Movie identifier to query customized aggregator endpoint.
     */
    fun startScrapingStreamUrl(movie: Movie) {
        _scrapeState.value = ScrapeUiState.Loading
        _activePlayingMovie.value = movie
        viewModelScope.launch {
            try {
                // Perform live scraping for .m3u8 sources
                val targetId = movie.id.toString() // String representation for the extractor
                val extractedStream = linkExtractor.extractM3u8Stream(
                    tmdbId = targetId,
                    aggregatorBaseUrl = _aggregatorUrl.value
                )
                _scrapeState.value = ScrapeUiState.Success(extractedStream)
            } catch (e: Exception) {
                // If the scrape fails, we check if pageUrlToScrape / videoUrl contains custom fallback to ensure beautiful video review is always ready
                if (movie.pageUrlToScrape.contains(".m3u8") || movie.videoUrl.contains(".m3u8")) {
                    _scrapeState.value = ScrapeUiState.Success(if (movie.videoUrl.contains(".m3u8")) movie.videoUrl else movie.pageUrlToScrape)
                } else {
                    _scrapeState.value = ScrapeUiState.Error("Servidor indisponível no momento. Tente outra fonte.")
                }
            }
        }
    }

    fun startDirectPlayback(fallbackUrl: String, movie: Movie) {
        _activePlayingMovie.value = movie
        _scrapeState.value = ScrapeUiState.Success(fallbackUrl)
    }

    fun startWhatsAppSubscriptionIntent(context: Context) {
        val uid = _userProfileId.value
        val formattedMsg = "Olá! Gostaria de assinar o plano VIP do app. Meu ID é: $uid"
        
        // Register current user as pending in administrative Room list for easier workflow
        registerVipRequested(uid, "Você (Dispositivo Local)")
        
        try {
            val encodedMsg = URLEncoder.encode(formattedMsg, "UTF-8")
            val waUri = Uri.parse("https://api.whatsapp.com/send?phone=5511999999999&text=$encodedMsg")
            val intent = Intent(Intent.ACTION_VIEW, waUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(formattedMsg, "UTF-8")}")
            val intent = Intent(Intent.ACTION_VIEW, browserUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun TmdbMovie.toDomainMovie(): Movie {
        val yearString = releaseDate?.split("-")?.firstOrNull() ?: "2026"
        val posterFullUrl = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500"
        val backdropFullUrl = if (backdropPath != null) "https://image.tmdb.org/t/p/w1280$backdropPath" else "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200"
        
        return Movie(
            id = (id % Int.MAX_VALUE).toInt(), // Safely convert Long id to Int for domains
            title = title,
            year = yearString,
            category = "TMDB Premium",
            rating = String.format("%.1f", voteAverage),
            isVIP = true, // TMDB movies are premium accessible
            resolution = "4K Ultra HD",
            duration = "2h",
            description = overview ?: "Nenhuma sinopse disponível.",
            accentTone = CineGold,
            posterUrl = posterFullUrl,
            bannerUrl = backdropFullUrl,
            pageUrlToScrape = "https://vidsrc.me/embed/movie?tmdb=$id",
            videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        )
    }
}
