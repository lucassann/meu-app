package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CineDarkGray
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineLightGray
import com.example.ui.theme.CineRed
import com.example.ui.theme.CineTextGray
import com.example.ui.theme.CineTextWhite
import com.example.ui.viewmodel.MovieViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Dashboard", "Usuários", "Conteúdo (CMS)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Painel do Administrador", color = CineGold, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CineDarkGray),
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = CineTextWhite)
                        }
                    }
                )
            },
            containerColor = Color.Black
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CineDarkGray,
                    contentColor = CineGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CineGold
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, color = if (selectedTab == index) CineGold else CineTextGray) }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> AdminStatsTab(viewModel)
                        1 -> AdminUsersTab(viewModel)
                        2 -> AdminContentTab(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatsTab(viewModel: MovieViewModel) {
    val vipUsers by viewModel.vipUsersList.collectAsState()
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Visão Geral do Sistema", color = CineTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            StatsCard(title = "Usuários Registrados", value = vipUsers.size.toString(), modifier = Modifier.weight(1f))
            StatsCard(title = "Online Agora", value = "Em Breve", modifier = Modifier.weight(1f))
        }
        
        // Push notification section here
    }
}

@Composable
fun StatsCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CineDarkGray)
            .border(1.dp, CineGold.copy(0.3f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = CineGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = CineTextGray, fontSize = 12.sp)
    }
}

@Composable
fun AdminUsersTab(viewModel: MovieViewModel) {
    val vipUsers by viewModel.vipUsersList.collectAsState()
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Gerenciamento de Clientes", color = CineTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vipUsers) { user ->
                Column(
                    modifier = Modifier.fillMaxWidth().background(CineDarkGray, RoundedCornerShape(8.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.name, color = CineTextWhite, fontWeight = FontWeight.Bold)
                            Text(user.id, color = CineTextGray, fontSize = 11.sp)
                            
                            val statusText = if (user.isBanned) "🚫 BANIDO" 
                                             else if (!user.isVip) "Inativo"
                                             else if (user.expirationTime == 0L) "VIP Vitalício"
                                             else "VIP até: " + java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(user.expirationTime))
                            Text(statusText, color = if (user.isBanned) CineRed else CineGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = user.isVip,
                            onCheckedChange = { 
                                val updatedUser = user.copy(isVip = it)
                                viewModel.updateVipUser(updatedUser) 
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = CineGold, checkedTrackColor = CineGold.copy(0.4f))
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { 
                                val updatedUser = user.copy(isBanned = !user.isBanned, isVip = false)
                                viewModel.updateVipUser(updatedUser) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (user.isBanned) CineLightGray else CineRed),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(if (user.isBanned) "Desbanir" else "Banir Dispositivo", fontSize = 11.sp, color = Color.White)
                        }
                        
                        Button(
                            onClick = { 
                                val newExp = if (user.expirationTime < System.currentTimeMillis()) System.currentTimeMillis() else user.expirationTime
                                val updatedUser = user.copy(expirationTime = newExp + 30L * 24 * 60 * 60 * 1000, isVip = true, isBanned = false)
                                viewModel.updateVipUser(updatedUser) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CineGold),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+30 Dias", fontSize = 11.sp, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminContentTab(viewModel: MovieViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.tmdbSearchResults.collectAsState()
    val customMovies by viewModel.customMovies.collectAsState()
    var movieToConfigure by remember { mutableStateOf<com.example.ui.screens.Movie?>(null) }
    var selectedType by remember { mutableStateOf("Filme") }
    var selectedGenre by remember { mutableStateOf("Ação") }
    var selectedSection by remember { mutableStateOf("Lançamentos") }
    var isVip by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Buscar no TMDB e Adicionar ao App", color = CineTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Nome do Filme/Série", color = CineTextGray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        
        Button(
            onClick = { viewModel.searchTmdbMovies(searchQuery) },
            colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar no TMDB")
        }
        
        if (searchResults.isNotEmpty()) {
            Text("Resultados da Busca:", color = CineTextWhite, fontSize = 14.sp)
            LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { movie ->
                    Row(
                        modifier = Modifier.fillMaxWidth().background(CineDarkGray, RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(movie.title, color = CineTextWhite, fontWeight = FontWeight.Bold)
                            Text(movie.year, color = CineTextGray, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { movieToConfigure = movie },
                            colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Configurar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text("Conteúdos Ativos no App (${customMovies.size})", color = CineTextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxHeight()) {
            items(customMovies) { movie ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(CineDarkGray, RoundedCornerShape(8.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(movie.title, color = CineTextWhite, fontWeight = FontWeight.Bold)
                        Text("${movie.type} • ${movie.section} • ${movie.category}", color = CineTextGray, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { 
                            selectedType = movie.type
                            selectedGenre = movie.category
                            selectedSection = movie.section
                            isVip = movie.isVIP
                            movieToConfigure = movie 
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = CineGold)
                        }
                        IconButton(onClick = { viewModel.deleteMovieFromCustomCatalog(movie.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remover", tint = CineRed)
                        }
                    }
                }
            }
        }
    }

    if (movieToConfigure != null) {
        Dialog(onDismissRequest = { movieToConfigure = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CineDarkGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Configurar: ${movieToConfigure?.title}", color = CineGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    
                    Text("Tipo", color = CineTextWhite, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Filme", "Série", "Anime", "Desenho")) { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type, color = CineTextWhite) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CineGold, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    Text("Gênero (Subcategoria)", color = CineTextWhite, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Ação", "Comédia", "Terror", "Drama", "Ficção", "Aventura", "Romance", "Suspense")) { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = { selectedGenre = genre },
                                label = { Text(genre, color = CineTextWhite) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CineGold, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    Text("Seção da Tela Inicial", color = CineTextWhite, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Lançamentos", "Top 10", "Mais Assistidos", "Populares", "Em Alta")) { sec ->
                            FilterChip(
                                selected = selectedSection == sec,
                                onClick = { selectedSection = sec },
                                label = { Text(sec, color = CineTextWhite) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CineGold, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Exclusivo VIP?", color = CineTextWhite)
                        Switch(checked = isVip, onCheckedChange = { isVip = it }, colors = SwitchDefaults.colors(checkedThumbColor = CineGold))
                    }
                    
                    var manualVideoUrl by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = manualVideoUrl,
                        onValueChange = { manualVideoUrl = it },
                        label = { Text("URL do Vídeo M3U8/MP4 (Opcional)", color = CineTextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val finalMovie = movieToConfigure!!.copy(
                                type = selectedType,
                                category = selectedGenre,
                                section = selectedSection,
                                isVIP = isVip,
                                videoUrl = if (manualVideoUrl.isNotBlank()) manualVideoUrl else movieToConfigure!!.videoUrl
                            )
                            viewModel.addMovieToCustomCatalog(finalMovie)
                            movieToConfigure = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Salvar no Aplicativo")
                    }
                }
            }
        }
    }
}
