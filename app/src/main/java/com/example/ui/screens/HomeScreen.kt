package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.CineBlack
import com.example.ui.theme.CineDarkGray
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGray
import com.example.ui.theme.CineLightGray
import com.example.ui.theme.CineRed
import com.example.ui.theme.CineTextGray
import com.example.ui.theme.CineTextWhite
import com.example.ui.theme.MyApplicationTheme
import com.example.data.CineRepository
import com.example.ui.viewmodel.MovieViewModel
import com.example.ui.viewmodel.ScrapeUiState
import kotlinx.coroutines.delay

// ---- DATA MODEL ----
data class Movie(
    val id: Int,
    val title: String,
    val year: String,
    val category: String,
    val rating: String,
    val isVIP: Boolean,
    val resolution: String = "4K Ultra HD",
    val duration: String,
    val description: String,
    val accentTone: Color,
    val posterUrl: String,
    val bannerUrl: String,
    val pageUrlToScrape: String = "https://cinepremium-stream-source.io/iframe-player/shae17", // Default mock page url to scrape
    val videoUrl: String = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8" // High-quality adaptive streaming fallback
)

// ---- MOCK DATA ----
object MovieMockData {
    val heroMovie = Movie(
        id = 99,
        title = "Crônicas Cósmicas: Interestelar 2026",
        year = "2026",
        category = "Ficção Científica",
        rating = "9.9",
        isVIP = true,
        duration = "2h 42m",
        description = "Em um universo em colapso, uma tripulação de elite tenta cruzar um buraco de minhoca recém-descoberto nas fronteiras de Saturno para salvar a consciência humana.",
        accentTone = Color(0xFF1E88E5),
        posterUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=600&auto=format&fit=crop&q=80",
        bannerUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1200&auto=format&fit=crop&q=80",
        pageUrlToScrape = "https://archive.org/embed/nasa-cosmic-collection-v5", // Scrapable archive iframe containing media links
        videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
    )

    val lancamentos = listOf(
        Movie(
            id = 1,
            title = "O Último Cyberpunk",
            year = "2026",
            category = "Ação",
            rating = "9.5",
            isVIP = false,
            resolution = "4K UHD",
            duration = "1h 58m",
            description = "Neon, chuva artificial e conspirações governamentais em Neo-Atalaia.",
            accentTone = Color(0xFF00E5FF),
            posterUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://player.vimeo.com/video/838183204", // Vimeo iframe holding sample assets
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        Movie(
            id = 2,
            title = "Duna: Deserto Cósmico",
            year = "2026",
            category = "Épico / Sci-Fi",
            rating = "9.7",
            isVIP = true,
            resolution = "IMAX 4K",
            duration = "2h 35m",
            description = "A batalha final pela energia cósmica concentrada nas areias áridas.",
            accentTone = Color(0xFFFFB300),
            posterUrl = "https://images.unsplash.com/photo-1547234935-80c7145ec969?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://player.vimeo.com/video/57663412",
            videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        ),
        Movie(
            id = 3,
            title = "Refúgio Silencioso",
            year = "2025",
            category = "Suspense",
            rating = "8.9",
            isVIP = false,
            resolution = "HDR10",
            duration = "1h 44m",
            description = "O silêncio absoluto é a única garantia de sobrevivência na floresta isolada.",
            accentTone = Color(0xFF8D6E63),
            posterUrl = "https://images.unsplash.com/photo-1505686994434-e3cc5abf1330?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://cinepremium-stream-source.io/iframe-player/shae17",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        ),
        Movie(
            id = 4,
            title = "A Ascensão do Castelo",
            year = "2026",
            category = "Fantasia",
            rating = "9.6",
            isVIP = true,
            resolution = "4K UHD",
            duration = "2h 10m",
            description = "Feitiços perdidos e batalhas medievais ganham vida em alta resolução.",
            accentTone = Color(0xFF7E57C2),
            posterUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://player.vimeo.com/video/57663412",
            videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        )
    )

    val recomendados = listOf(
        Movie(
            id = 5,
            title = "Câmera Escura",
            year = "2025",
            category = "Terror Psicológico",
            rating = "9.1",
            isVIP = false,
            resolution = "HDR",
            duration = "1h 37m",
            description = "Uma câmera antiga revela os segredos obscuros enterrados no porão.",
            accentTone = Color(0xFFD32F2F),
            posterUrl = "https://images.unsplash.com/photo-1485594050903-8e8ee7b071a8?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1485594050903-8e8ee7b071a8?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://cinepremium-stream-source.io/iframe-player/terror2",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        ),
        Movie(
            id = 6,
            title = "Cinquenta Graus Norte",
            year = "2025",
            category = "Aventura / Drama",
            rating = "9.2",
            isVIP = false,
            resolution = "4K UHD",
            duration = "2h 05m",
            description = "Uma jornada gelada de sobrevivência pelos picos mais hostis do Alasca.",
            accentTone = Color(0xFF26A69A),
            posterUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://player.vimeo.com/video/57663412",
            videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        ),
        Movie(
            id = 7,
            title = "O Homem de Ferro de Tóquio",
            year = "2026",
            category = "Animação / Ação",
            rating = "9.8",
            isVIP = true,
            resolution = "Dolby Vision",
            duration = "1h 52m",
            description = "O herói mecanizado de Tóquio levanta-se contra as corporações sombrias.",
            accentTone = Color(0xFFEC407A),
            posterUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200&auto=format&fit=crop&q=80",
            pageUrlToScrape = "https://cinepremium-stream-source.io/iframe-player/mecha8",
            videoUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"
        ),
        Movie(
            id = 8,
            title = "Rivalidade na Pista",
            year = "2025",
            category = "Documentário",
            rating = "8.8",
            isVIP = false,
            resolution = "Full HD",
            duration = "1h 30m",
            description = "Por trás dos bastidores rápidos da temporada mais acirrada do automobilismo.",
            accentTone = Color(0xFF82B1FF),
            posterUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=500&auto=format&fit=crop&q=80",
            bannerUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=1200&auto=format&fit=crop&q=80"
        )
    )
}

// ---- MAIN SCREEN COMPOSABLE ----
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MovieViewModel? = null
) {
    var selectedTab by remember { mutableStateOf("Início") }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val viewModel = viewModel ?: remember {
        MovieViewModel(CineRepository(context.applicationContext))
    }
    
    // Observers using Compose Flow state collections
    val isUserVip by viewModel.isUserVip.collectAsState()
    val scrapeUiState by viewModel.scrapeState.collectAsState()
    val activePlayingMovie by viewModel.activePlayingMovie.collectAsState()
    
    // TMDB and Admin state hooks
    val popularMovies by viewModel.popularMovies.collectAsState()
    val trendingMovies by viewModel.trendingMovies.collectAsState()
    val topRatedMovies by viewModel.topRatedMovies.collectAsState()
    val isCatalogLoading by viewModel.isCatalogLoading.collectAsState()
    val userProfileId by viewModel.userProfileId.collectAsState()
    val vipUsersList by viewModel.vipUsersList.collectAsState()
    val aggregatorUrl by viewModel.aggregatorUrl.collectAsState()
    val tmdbApiKey by viewModel.tmdbApiKey.collectAsState()
    val customMovies by viewModel.customMovies.collectAsState()

    var showAdminPanel by remember { mutableStateOf(false) }
    var showPasscodeDialog by remember { mutableStateOf(false) }
    var logoClickCount by remember { mutableStateOf(0) }
    
    // Toast UI error handling for Jsoup failure as required
    LaunchedEffect(scrapeUiState) {
        if (scrapeUiState is ScrapeUiState.Error) {
            val errorMsg = (scrapeUiState as ScrapeUiState.Error).errorMsg
            android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.setActiveMovie(null) // Safe recovery to idle state
        }
    }
    
    // Local dialogs controls
    var selectedMovieForDetails by remember { mutableStateOf<Movie?>(null) }
    var showPaywallDialog by remember { mutableStateOf(false) }
    var showLoginAuth by remember { mutableStateOf(false) }
    var movieDeferredToPlay by remember { mutableStateOf<Movie?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Automatically dismiss loading after 1.5 seconds to show transition
    LaunchedEffect(Unit) {
        delay(1500)
        isLoading = false
    }

    // Centered Action to handle movie media playback check with VIP lock
    val handlePlayRequest = { movie: Movie ->
        if (movie.isVIP && !isUserVip) {
            // Access Blocked: User is not VIP! Trigger premium billing paywall dialog
            movieDeferredToPlay = movie
            showPaywallDialog = true
        } else {
            // Access Unlocked: Live scrape from agregador via TMDB context
            viewModel.setActiveMovie(movie)
            viewModel.startScrapingStreamUrl(movie)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CineBlack,
        bottomBar = {
            FloatingBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CineBlack)
        ) {
            
            // --- TAB SELECTOR INNER BODY ---
            when (selectedTab) {
                "Início" -> {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(bottom = 110.dp)
                    ) {
                        if (isLoading) {
                            ShimmerSkeleton(onForceLoadClick = { isLoading = false })
                        } else {
                            // High altitude Hero banner
                            HeroSection(
                                movie = MovieMockData.heroMovie,
                                onPlayClick = { handlePlayRequest(MovieMockData.heroMovie) },
                                onMyListClick = { /* Saved to user favorites list */ },
                                isLoading = (activePlayingMovie == MovieMockData.heroMovie && scrapeUiState is ScrapeUiState.Loading)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (isCatalogLoading) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = CineGold)
                                }
                            } else {
                                MovieCarousel(
                                    title = "Adicionados Recentemente (CMS)",
                                    movies = customMovies,
                                    onMovieSelect = { selectedMovieForDetails = it }
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                MovieCarousel(
                                    title = "Lançamentos e Populares",
                                    movies = popularMovies,
                                    onMovieSelect = { selectedMovieForDetails = it }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Dynamic AdMob banner simulation for nonvip users
                                if (!isUserVip) {
                                    AdMobBanner()
                                    Spacer(modifier = Modifier.height(18.dp))
                                }

                                MovieCarousel(
                                    title = "Tendências da Semana (TMDB)",
                                    movies = trendingMovies.ifEmpty { MovieMockData.recomendados },
                                    onMovieSelect = { selectedMovieForDetails = it }
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                MovieCarousel(
                                    title = "Melhores Avaliados (TMDB)",
                                    movies = topRatedMovies.ifEmpty { MovieMockData.lancamentos.reversed() },
                                    onMovieSelect = { selectedMovieForDetails = it }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
                
                "Explorar" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .padding(bottom = 90.dp)
                    ) {
                        Text(
                            text = "Explorar",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Dynamic glass search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar filmes, diretores, categorias...", color = CineTextGray) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = CineTextGray) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CineDarkGray,
                                unfocusedContainerColor = CineDarkGray,
                                focusedBorderColor = CineRed,
                                unfocusedBorderColor = CineLightGray,
                                focusedTextColor = CineTextWhite,
                                unfocusedTextColor = CineTextWhite
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Category Pills
                        val categories = listOf("Todos", "Ação", "Sci-Fi", "Suspense", "Terror", "Épicos")
                        var selectedCategory by remember { mutableStateOf("Todos") }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { cat ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selectedCategory == cat) CineRed else CineDarkGray)
                                        .clickable { selectedCategory = cat }
                                        .border(1.dp, if (selectedCategory == cat) Color.Transparent else CineLightGray, RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(text = cat, color = CineTextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Filter mock list based on searches
                        val allMovies = MovieMockData.lancamentos + MovieMockData.recomendados + listOf(MovieMockData.heroMovie)
                        val filteredMovies = allMovies.filter {
                            (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
                            (selectedCategory == "Todos" || it.category.contains(selectedCategory, ignoreCase = true))
                        }.distinctBy { it.id }

                        if (filteredMovies.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Nenhum filme coincide com os filtros.", color = CineTextGray, fontSize = 14.sp)
                            }
                        } else {
                            // Grid List
                            val scrollStateGrid = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(scrollStateGrid),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                filteredMovies.forEach { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = { selectedMovieForDetails = movie },
                                        modifier = Modifier.fillMaxHeight(0.85f).align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                    }
                }
                
                "VIP" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .padding(bottom = 110.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title Header
                        Text(
                            text = "Assinatura VIP",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        // Member current status indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(CineDarkGray, CineBlack)))
                                .border(1.2.dp, if (isUserVip) CineGold else CineRed, RoundedCornerShape(16.dp))
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "SEU PLANO ATUAL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CineTextGray,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isUserVip) "VIP CinePremium ★" else "Acesso Grátis com Anúncios",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isUserVip) CineGold else CineTextWhite
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (isUserVip) CineGold.copy(0.15f) else CineRed.copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isUserVip) Icons.Filled.Star else Icons.Filled.Lock,
                                        contentDescription = "Status",
                                        tint = if (isUserVip) CineGold else CineRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Plan details card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CineDarkGray),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Benefícios Premium VIP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite
                                )
                                
                                val benefits = listOf(
                                    "Acesso ilimitado a todos os filmes em ultra resolução (4K & IMAX).",
                                    "Sem anúncios publicitários irritantes interrompendo.",
                                    "Transmissão HLS adaptiva de alta performance instantânea.",
                                    "Suporte VIP dedicado via WhatsApp prioritário."
                                )
                                benefits.forEach { benefit ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("✔", color = CineGold, fontWeight = FontWeight.Black)
                                        Text(text = benefit, color = CineTextGray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // WhatsApp Action Trigger as required by specifications
                        Button(
                            onClick = { viewModel.startWhatsAppSubscriptionIntent(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366), // Standard Whatsapp color signature
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("whatsapp_subscribe_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = "WhatsApp Contact",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Falar com Atendente no WhatsApp",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Switch toggle for simple local manual configuration check
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = CineDarkGray.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CineLightGray, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Simular VIP Localmente",
                                        color = CineTextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ative para testar todos os players imediatamente sem paywall",
                                        color = CineTextGray,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                Switch(
                                    checked = isUserVip,
                                    onCheckedChange = { viewModel.setVipStatus(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CineGold,
                                        checkedTrackColor = CineGold.copy(0.4f),
                                        uncheckedThumbColor = CineTextGray,
                                        uncheckedTrackColor = CineLightGray
                                    ),
                                    modifier = Modifier.testTag("vip_local_switch")
                                )
                            }
                        }
                    }
                }
                
                "Perfil" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .padding(bottom = 110.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Meu Perfil",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(CineDarkGray)
                                .border(2.dp, CineRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = CineTextWhite,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Text(
                            text = "Premium User",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite
                        )
                        
                        Text(
                            text = "Email: vip-tester@cinepremium.com",
                            fontSize = 13.sp,
                            color = CineTextGray
                        )

                        // Unique Session Code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(CineLightGray)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            val randomIdForShow = remember { (1000..9999).random() }
                            Text(
                                text = "ID do Cliente: #CINE-$randomIdForShow",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CineGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // General Mock options list representation
                        val settingsOptions = listOf(
                            Pair(Icons.Default.Settings, "Configurações da Conta"),
                            Pair(Icons.Default.Notifications, "Preferências de Notificação"),
                            Pair(Icons.Default.Info, "Sobre o App & Licenças")
                        )
                        settingsOptions.forEach { opt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CineDarkGray)
                                    .clickable { /* Perform Mock Action */ }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = opt.first, contentDescription = opt.second, tint = CineTextGray, modifier = Modifier.size(18.dp))
                                Text(text = opt.second, color = CineTextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // Top screen Header (Immersive black-transparent overlay to match design)
            if (selectedTab == "Início") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                0.0f to CineBlack.copy(alpha = 0.9f),
                                0.6f to CineBlack.copy(alpha = 0.5f),
                                1.0f to Color.Transparent
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo CinePremium with stylized icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                logoClickCount++
                                if (logoClickCount >= 10) {
                                    logoClickCount = 0
                                    showPasscodeDialog = true
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CineRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play Logo",
                                    tint = CineTextWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CINEPREMIUM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CineTextWhite,
                                    letterSpacing = 2.sp,
                                    shadow = Shadow(color = Color.Black, blurRadius = 3f)
                                )
                            )
                        }

                        if (showLoginAuth) {
                            LoginAuthScreen(
                                onDismiss = { showLoginAuth = false },
                                onLoginSuccess = { uid ->
                                    showLoginAuth = false
                                    viewModel.setVipStatus(true)
                                }
                            )
                        }

                        if (!isUserVip) {
                            Button(
                                onClick = { showLoginAuth = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CineGold.copy(alpha = 0.2f),
                                    contentColor = CineGold
                                ),
                                shape = RoundedCornerShape(30.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, CineGold),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Login Premium",
                                    modifier = Modifier.size(14.dp),
                                    tint = CineGold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Acesso Premium",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(CineGold)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Premium Ativo", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: DETAILED MOVIE INFO ---
    if (selectedMovieForDetails != null) {
        val movie = selectedMovieForDetails!!
        AlertDialog(
            onDismissRequest = { selectedMovieForDetails = null },
            containerColor = CineDarkGray,
            title = null,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(movie.bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, CineBlack.copy(0.85f))
                                    )
                                )
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = movie.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = CineTextWhite,
                            modifier = Modifier.weight(0.7f)
                        )
                        if (movie.isVIP) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CineGold)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "VIP ★",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Text(
                        text = "${movie.year}  •  ${movie.category}  •  ${movie.duration}  •  ★ ${movie.rating}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineRed
                    )

                    Text(
                        text = movie.description,
                        fontSize = 13.sp,
                        color = CineTextGray,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMovieForDetails = null
                        handlePlayRequest(movie)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineRed, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("details_play_button")
                ) {
                    Text("Assistir Agora", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedMovieForDetails = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("Voltar")
                }
            }
        )
    }

    // --- DIALOG: JSOUP SCRAPER LOADING PROCESS ---
    if (scrapeUiState is ScrapeUiState.Loading) {
        AlertDialog(
            onDismissRequest = { /* Prevent cancellation to maintain state sequence */ },
            containerColor = CineDarkGray,
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = CineRed,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = "Raspando link do servidor seguro...",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CineTextWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "O Jsoup está escaneando a página carregada em background buscando por endereços HLS .m3u8 válidos.",
                        fontSize = 12.sp,
                        color = CineTextGray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {}
        )
    }

    // --- DIALOG: MONETIZATION PREMIUM PLAN PAYWALL BARRIER ---
    if (showPaywallDialog && movieDeferredToPlay != null) {
        val movie = movieDeferredToPlay!!
        AlertDialog(
            onDismissRequest = { showPaywallDialog = false },
            containerColor = CineDarkGray,
            modifier = Modifier.testTag("paywall_container"),
            title = {
                Text(
                    text = "Área Premium VIP ★",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = CineGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "O filme \"${movie.title}\" requer uma assinatura VIP ativa para visualização em alta resolução.",
                        fontSize = 14.sp,
                        color = CineTextWhite,
                        textAlign = TextAlign.Center
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CineLightGray)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Benefícios VIP inclusos na contratação:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CineTextWhite
                            )
                            Text(text = "• Transmissão 4K Ultra HD sem travamentos.", fontSize = 11.sp, color = CineTextGray)
                            Text(text = "• Sem anúncios ou comerciais importunos.", fontSize = 11.sp, color = CineTextGray)
                            Text(text = "• Ativação automática no sistema do app.", fontSize = 11.sp, color = CineTextGray)
                        }
                    }

                    Text(
                        text = "Clique abaixo para falar diretamente conosco via WhatsApp de vendas e obter o comprovante de ativação.",
                        fontSize = 12.sp,
                        color = CineTextGray,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Purchase button (Triggers WhatsApp implicit intent as requested)
                    Button(
                        onClick = {
                            showPaywallDialog = false
                            viewModel.startWhatsAppSubscriptionIntent(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("paywall_subscribe_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.Call, contentDescription = "WhatsApp", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assinar VIP pelo WhatsApp", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }

                    // Simulated manual bypass button (allows full testing of ExoPlayer / stream parsing instantly)
                    Button(
                        onClick = {
                            showPaywallDialog = false
                            viewModel.setVipStatus(true)
                            // Re-trigger play request since user simulated VIP now
                            handlePlayRequest(movie)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("paywall_demo_button")
                    ) {
                        Text("Demonstrar ExoPlayer (Ativar VIP Grátis)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showPaywallDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CineTextGray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("Voltar ao Catálogo")
                    }
                }
            }
        )
    }

    // --- FULL-SCREEN PLAYER SCREEN OVERLAY PLAYBACK ENHANCEMENT ---
    if (activePlayingMovie != null && scrapeUiState is ScrapeUiState.Success) {
        val streamUrl = (scrapeUiState as ScrapeUiState.Success).streamUrl
        VideoPlayerScreen(
            videoUrl = streamUrl,
            onCloseClick = { viewModel.setActiveMovie(null) },
            modifier = Modifier.fillMaxSize()
        )
    }

    // --- PASSCODE SECRETO GESTURE OVERLAY ---
    if (showPasscodeDialog) {
        var pinInput by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showPasscodeDialog = false },
            containerColor = CineDarkGray,
            title = {
                Text("Cadeado Secreto 🔒", color = CineGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Digite a senha secreta do Administrador:", color = CineTextWhite, fontSize = 13.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { 
                            pinInput = it
                            isError = false
                        },
                        label = { Text("Senha PIN", color = CineTextGray) },
                        singleLine = true,
                        isError = isError,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CineGold,
                            unfocusedBorderColor = CineLightGray
                        )
                    )
                    if (isError) {
                        Text("Senha incorreta. Tente novamente.", color = CineRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.trim() == "Lucas123@" || pinInput.trim() == "admin") {
                            showPasscodeDialog = false
                            showAdminPanel = true
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black)
                ) {
                    Text("Desbloquear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasscodeDialog = false }) {
                    Text("Cancelar", color = CineTextGray)
                }
            }
        )
    }
    if (showAdminPanel) {
        AdminDashboardScreen(
            viewModel = viewModel,
            onDismiss = { showAdminPanel = false }
        )
    }
}

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CineDarkGray)
            .border(BorderStroke(1.dp, CineGold.copy(alpha = 0.2f)), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("admob_banner_placeholder")
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(CineGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("ANÚNCIO ADMOB", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Campanha Patrocinada", color = CineTextGray, fontSize = 10.sp)
                }
                Text(text = "ⓘ", color = CineTextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "⚡ CinePremium VIP - Ativação Instantânea!",
                color = CineTextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Assine pelo WhatsApp para obter players de vídeo com trava de tela e ajuste de volume/brilho por gestos! Clique em ASSINAR no player para falar conosco.",
                color = CineTextGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

// ---- HERO SECTION COMPOSABLE (HEADER BANNER) ----
@Composable
fun HeroSection(
    movie: Movie,
    onPlayClick: () -> Unit,
    onMyListClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    var isSavedToList by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(450.dp)
    ) {
        // High-altitude cinematic covers
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.bannerUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Capa de ${movie.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .testTag("hero_banner_image"),
            error = painterResource(id = android.R.drawable.ic_menu_gallery),
            alpha = 0.9f
        )

        // Multiple overlays to construct absolute deep Cinematic Depth (Fades cleanly to coal-black background)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to CineBlack.copy(alpha = 0.2f),
                        0.7f to CineBlack.copy(alpha = 0.6f),
                        0.9f to CineBlack.copy(alpha = 0.9f),
                        1.0f to CineBlack
                    )
                )
        )

        // Movie contextual metadata and actions (Asymmetric modern Netflix/Apple TV style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Subtle premium tag marker (e.g. VIP label or IMAX badge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                // RED BADGE (from HTML: px-2 py-0.5 bg-red-600 text-[10px] font-black uppercase rounded-sm)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(CineRed)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "TOP 10",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "${movie.year}  •  ${movie.category}  •  ${movie.resolution}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = CineTextGray,
                    letterSpacing = 0.5.sp
                )
            }

            // Bold typography display title
            Text(
                text = movie.title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite,
                textAlign = TextAlign.Start,
                lineHeight = 38.sp,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("hero_title"),
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(color = Color.Black, blurRadius = 8f)
                )
            )

            // High priority actionable control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Assistir Agora Button (Primary high-density button with simulated loading)
                Button(
                    onClick = { if (!isLoading) onPlayClick() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_play_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp).testTag("hero_play_loading"),
                                color = CineRed,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buscando Fonte...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        } else {
                            Text(
                                text = "▶",
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Assistir Agora",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Minha Lista Button (Secondary translucent modern circular button)
                Button(
                    onClick = { 
                        isSavedToList = !isSavedToList 
                        onMyListClick()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("hero_list_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSavedToList) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = "Minha Lista",
                            modifier = Modifier.size(24.dp),
                            tint = if (isSavedToList) CineGold else Color.White
                        )
                    }
                }
            }
        }
    }
}

// ---- CATEGORY CAROUSEL (LAZYROW) ----
@Composable
fun MovieCarousel(
    title: String,
    movies: List<Movie>,
    onMovieSelect: (Movie) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Carousel section title with premium red accent block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CineRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Ver tudo",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray,
                modifier = Modifier.clickable { /* See all */ }
            )
        }

        // Horizontal LazyRow containing movies cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onMovieSelect(movie) }
                )
            }
        }
    }
}

// ---- INDIVIDUAL MOVIE CARD ----
@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(128.dp)
            .aspectRatio(2f / 3f)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("movie_card_${movie.id}")
            .background(CineDarkGray)
    ) {
        // Movie poster from URL with Coil
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(movie.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = painterResource(id = android.R.drawable.ic_menu_gallery)
        )

        // Bottom subtle scrim shadow to outline text inside card if visual layout fails
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.6f to CineBlack.copy(alpha = 0.3f),
                        0.8f to CineBlack.copy(alpha = 0.7f),
                        1.0f to CineBlack.copy(alpha = 0.95f)
                    )
                )
        )

        // Rating or luxury indicators
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            if (movie.isVIP) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CineGold.copy(alpha = 0.95f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIP",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = CineBlack,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = CineGold,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = movie.rating,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = CineTextWhite,
                        )
                    }
                }
            }
        }

        // Compact details overlayed inside card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Text(
                text = movie.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CineTextWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${movie.year} • ${movie.category}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = CineTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---- SHIMMER METRICS SKELETON LOADER (PREMIUM LOADING STATE) ----
@Composable
fun ShimmerSkeleton(
    onForceLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // Shimmer Brush builder
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            CineDarkGray,
            CineGray,
            CineLightGray,
            CineGray,
            CineDarkGray
        ),
        start = Offset(translateAnim - 250f, translateAnim - 250f),
        end = Offset(translateAnim, translateAnim)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("shimmer_skeleton")
    ) {
        // Hero Section Shimmer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(shimmerBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CineBlack.copy(alpha = 0.5f), CineBlack)
                        )
                    )
            )

            // Centered loader state hints inside the skeleton to maximize readability
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )

                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmerBrush)
                )

                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // First Row Skeleton Carousels
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CineRed)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }

            // Cards list shimmer
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(128.dp)
                            .aspectRatio(2f / 3f)
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .border(width = 1.dp, color = Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

// ---- FLOATING BOTTOM NAVIGATION BAR COMPOSABLE ----
@Composable
fun FloatingBottomNavigationBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Safety offsets to defend against bottom gestural pill overlap
            .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.7f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(CineDarkGray.copy(alpha = 0.9f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    label = "Início",
                    icon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    isSelected = selectedTab == "Início",
                    onClick = { onTabSelected("Início") }
                )

                BottomNavItem(
                    label = "Explorar",
                    icon = Icons.Filled.Search,
                    unselectedIcon = Icons.Outlined.Search,
                    isSelected = selectedTab == "Explorar",
                    onClick = { onTabSelected("Explorar") }
                )

                BottomNavItem(
                    label = "VIP",
                    icon = Icons.Filled.Star,
                    unselectedIcon = Icons.Outlined.Star,
                    isSelected = selectedTab == "VIP",
                    activeColor = CineGold,
                    onClick = { onTabSelected("VIP") }
                )

                BottomNavItem(
                    label = "Perfil",
                    icon = Icons.Filled.Person,
                    unselectedIcon = Icons.Outlined.Person,
                    isSelected = selectedTab == "Perfil",
                    onClick = { onTabSelected("Perfil") }
                )
            }
        }
    }
}

// ---- BOTTOM NAV ITEM HELPER ----
@Composable
fun BottomNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .testTag("nav_item_$label"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active pill: w-12 h-8 flex items-center justify-center bg-white/10 rounded-full
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) icon else unselectedIcon,
                contentDescription = label,
                tint = if (isSelected) activeColor else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.45f),
            letterSpacing = 1.2.sp
        )
    }
}

// ---- DESIGN PREVIEWS ----
@Preview(showBackground = true, device = "spec:width=1080px,height=2340px,dpi=440")
@Composable
fun HomeScreenNormalPreview() {
    MyApplicationTheme {
        HomeScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun HeroSectionPreview() {
    MyApplicationTheme {
        HeroSection(
            movie = MovieMockData.heroMovie,
            onPlayClick = {},
            onMyListClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ShimmerSkeletonPreview() {
    MyApplicationTheme {
        Surface(color = CineBlack, modifier = Modifier.fillMaxSize()) {
            ShimmerSkeleton(onForceLoadClick = {})
        }
    }
}
