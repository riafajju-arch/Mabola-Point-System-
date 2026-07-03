package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MpsViewModel
import kotlinx.coroutines.launch
import java.util.*

// Custom helper to provide premium Black and Gold colors across all text fields
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mpsTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MpsWhite,
    unfocusedTextColor = MpsWhite,
    focusedBorderColor = MpsGold,
    unfocusedBorderColor = MpsMediumGray,
    focusedLabelColor = MpsGold,
    unfocusedLabelColor = MpsLightGray,
    focusedContainerColor = MpsBlack,
    unfocusedContainerColor = MpsBlack
)

// Custom high-contrast filter chips to bypass Material3 version differences
@Composable
fun MpsFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MpsGold else MpsMediumGray)
            .border(1.dp, if (selected) MpsGold else MpsLightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) MpsBlack else MpsWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MpsViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentAdminScreen by viewModel.currentAdminScreen.collectAsStateWithLifecycle()
    val loggedInAdmin by viewModel.loggedInAdmin.collectAsStateWithLifecycle()

    val players by viewModel.players.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val pointConfig by viewModel.pointSystemConfig.collectAsStateWithLifecycle()
    val adminConfig by viewModel.adminConfig.collectAsStateWithLifecycle()

    // Screen dimensions wrapper for adaptive layouts
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MpsBlack)) {
        val isWide = maxWidth >= 700.dp
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SportsCricket,
                                contentDescription = "MPS Logo",
                                tint = MpsGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MABOLA POINT SYSTEM",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MpsWhite,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MpsBlack,
                        titleContentColor = MpsWhite
                    ),
                    actions = {
                        if (loggedInAdmin != null) {
                            IconButton(
                                onClick = {
                                    viewModel.logout()
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("admin_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Logout,
                                    contentDescription = "Logout",
                                    tint = MpsRed
                                )
                            }
                        } else {
                            if (currentScreen != "Login") {
                                Button(
                                    onClick = { viewModel.navigateTo("Login") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 8.dp).testTag("admin_login_nav_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = "Admin Login",
                                        tint = MpsGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Admin", color = MpsWhite, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // Show bottom navigation bar on mobile/compact screens (if NOT logged in as admin)
                if (!isWide && loggedInAdmin == null) {
                    NavigationBar(
                        containerColor = MpsDarkGray,
                        tonalElevation = 8.dp
                    ) {
                        val navItems = listOf(
                            Triple("Home", Icons.Default.Home, "Home"),
                            Triple("Rankings", Icons.Default.Leaderboard, "Rankings"),
                            Triple("Teams", Icons.Default.Groups, "Teams"),
                            Triple("Search", Icons.Default.Search, "Search")
                        )
                        navItems.forEach { (screenName, icon, label) ->
                            NavigationBarItem(
                                selected = currentScreen == screenName,
                                onClick = { viewModel.navigateTo(screenName) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MpsBlack,
                                    selectedTextColor = MpsGold,
                                    indicatorColor = MpsGold,
                                    unselectedIconColor = MpsLightGray,
                                    unselectedTextColor = MpsLightGray
                                )
                            )
                        }
                    }
                } else if (!isWide && loggedInAdmin != null) {
                    // Mobile navigation for logged in Admin
                    NavigationBar(
                        containerColor = MpsDarkGray,
                        tonalElevation = 8.dp
                    ) {
                        val adminItems = listOf(
                            Triple("Dashboard", Icons.Default.Dashboard, "Dash"),
                            Triple("Players", Icons.Default.Person, "Players"),
                            Triple("Teams", Icons.Default.Groups, "Teams"),
                            Triple("Matches", Icons.Default.SportsCricket, "Matches"),
                            Triple("Settings", Icons.Default.Settings, "Config")
                        )
                        adminItems.forEach { (screenName, icon, label) ->
                            NavigationBarItem(
                                selected = currentAdminScreen == screenName,
                                onClick = { viewModel.navigateAdminTo(screenName) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MpsBlack,
                                    selectedTextColor = MpsGold,
                                    indicatorColor = MpsGold,
                                    unselectedIconColor = MpsLightGray,
                                    unselectedTextColor = MpsLightGray
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive Side navigation on wide screens (tablets / expanded views)
                if (isWide) {
                    if (loggedInAdmin == null) {
                        NavigationRail(
                            containerColor = MpsDarkGray,
                            header = {
                                Text(
                                    "PUBLIC",
                                    fontWeight = FontWeight.Bold,
                                    color = MpsGold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        ) {
                            val railItems = listOf(
                                Triple("Home", Icons.Default.Home, "Home"),
                                Triple("Rankings", Icons.Default.Leaderboard, "Rankings"),
                                Triple("Teams", Icons.Default.Groups, "Teams"),
                                Triple("Search", Icons.Default.Search, "Search"),
                                Triple("About", Icons.Default.Info, "Rules")
                            )
                            railItems.forEach { (screenName, icon, label) ->
                                NavigationRailItem(
                                    selected = currentScreen == screenName,
                                    onClick = { viewModel.navigateTo(screenName) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MpsBlack,
                                        selectedTextColor = MpsGold,
                                        indicatorColor = MpsGold,
                                        unselectedIconColor = MpsLightGray,
                                        unselectedTextColor = MpsLightGray
                                    )
                                )
                            }
                        }
                    } else {
                        NavigationRail(
                            containerColor = MpsDarkGray,
                            header = {
                                Text(
                                    "ADMIN",
                                    fontWeight = FontWeight.Bold,
                                    color = MpsGold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        ) {
                            val adminRailItems = listOf(
                                Triple("Dashboard", Icons.Default.Dashboard, "Dashboard"),
                                Triple("Players", Icons.Default.Person, "Players"),
                                Triple("Teams", Icons.Default.Groups, "Teams"),
                                Triple("Matches", Icons.Default.SportsCricket, "Matches"),
                                Triple("Settings", Icons.Default.Settings, "Settings")
                            )
                            adminRailItems.forEach { (screenName, icon, label) ->
                                NavigationRailItem(
                                    selected = currentAdminScreen == screenName,
                                    onClick = { viewModel.navigateAdminTo(screenName) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MpsBlack,
                                        selectedTextColor = MpsGold,
                                        indicatorColor = MpsGold,
                                        unselectedIconColor = MpsLightGray,
                                        unselectedTextColor = MpsLightGray
                                    )
                                )
                            }
                        }
                    }
                }

                // Main screen routing container
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    if (loggedInAdmin != null) {
                        // Admin Views
                        when (currentAdminScreen) {
                            "Dashboard" -> AdminDashboardScreen(viewModel, players, teams, matches)
                            "Players" -> AdminPlayersScreen(viewModel, players, teams)
                            "Teams" -> AdminTeamsScreen(viewModel, teams)
                            "Matches" -> AdminMatchesScreen(viewModel, matches, teams, players)
                            "Settings" -> AdminSettingsScreen(viewModel, pointConfig ?: PointSystemConfigEntity(), adminConfig ?: AdminConfigEntity())
                            else -> AdminDashboardScreen(viewModel, players, teams, matches)
                        }
                    } else {
                        // Public Views
                        when (currentScreen) {
                            "Home" -> PublicHomeScreen(viewModel, players, teams, matches)
                            "Rankings" -> PublicRankingsScreen(viewModel, players)
                            "Teams" -> PublicTeamsScreen(viewModel, teams, players)
                            "Search" -> PublicSearchScreen(viewModel, players, teams, matches)
                            "About" -> PublicAboutScreen(pointConfig ?: PointSystemConfigEntity())
                            "Login" -> AdminLoginScreen(viewModel)
                            else -> PublicHomeScreen(viewModel, players, teams, matches)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PUBLIC VIEW SCREENS
// ==========================================

@Composable
fun PublicHomeScreen(
    viewModel: MpsViewModel,
    players: List<PlayerEntity>,
    teams: List<TeamEntity>,
    matches: List<MatchEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Visual Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MpsGold)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MpsMediumGray, MpsBlack)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "MABOLA POINT SYSTEM",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MpsGold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Premium Cricket scoring, automated leaderboards, and live stats tracking.",
                            fontSize = 14.sp,
                            color = MpsWhite
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.navigateTo("Rankings") },
                                colors = ButtonDefaults.buttonColors(containerColor = MpsGold),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("View Standings", color = MpsBlack, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.navigateTo("Search") },
                                colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Search Players", color = MpsWhite)
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Summary
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSumCard(Modifier.weight(1f), "TEAMS", teams.size.toString(), Icons.Default.Groups)
                StatSumCard(Modifier.weight(1f), "PLAYERS", players.size.toString(), Icons.Default.Person)
                StatSumCard(Modifier.weight(1f), "MATCHES", matches.size.toString(), Icons.Default.SportsCricket)
            }
        }

        // Leaderboard Previews
        item {
            Text(
                "👑 CURRENT LEADERBOARD PREVIEW",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MpsGold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (players.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = "No data",
                            tint = MpsLightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No tournament data loaded yet.", color = MpsWhite, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Log in as Admin from the navigation panel to add players, teams, and record match scorecards.",
                            color = MpsLightGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Display top 3 players in a premium card list
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOP PERFORMERS", fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 14.sp)
                            TextButton(onClick = { viewModel.navigateTo("Rankings") }) {
                                Text("View All", color = MpsGold)
                            }
                        }
                        Divider(color = MpsMediumGray, modifier = Modifier.padding(vertical = 8.dp))
                        
                        players.take(3).forEachIndexed { index, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Rank Badge
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (index == 0) MpsGold else MpsMediumGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            (index + 1).toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) MpsBlack else MpsWhite,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 14.sp)
                                        Text("${p.teamName} • ${p.role}", color = MpsLightGray, fontSize = 11.sp)
                                    }
                                }
                                Text("${p.totalPoints.toInt()} PTS", fontWeight = FontWeight.Bold, color = MpsGold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatSumCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
        border = BorderStroke(0.5.dp, MpsMediumGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MpsGold, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MpsWhite)
            Text(label, fontSize = 10.sp, color = MpsLightGray, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PublicRankingsScreen(viewModel: MpsViewModel, players: List<PlayerEntity>) {
    var subTab by remember { mutableStateOf("Overall") }
    val sortedPlayers = remember(players, subTab) {
        when (subTab) {
            "Overall" -> players.sortedByDescending { it.totalPoints }
            "Batting" -> players.sortedByDescending { it.runsScored }
            "Bowling" -> players.sortedByDescending { it.wickets }
            "All-Rounders" -> players.filter { it.role.contains("All-Rounder", ignoreCase = true) }.sortedByDescending { it.totalPoints }
            "Fielding" -> players.sortedByDescending { it.catches * 8 + it.directRunOuts * 12 + it.runOutAssists * 6 + it.stumpings * 12 }
            else -> players.sortedByDescending { it.totalPoints }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🏏 TOURNAMENT LEADERBOARDS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
        Spacer(modifier = Modifier.height(12.dp))

        // Sub categories scrollable tabs
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Overall", "Batting", "Bowling", "All-Rounders", "Fielding").forEach { tab ->
                MpsFilterChip(
                    selected = subTab == tab,
                    onClick = { subTab = tab },
                    label = tab
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (sortedPlayers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No players match this category.", color = MpsLightGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedPlayers.take(100)) { p ->
                    val statText = when (subTab) {
                        "Overall" -> "${p.totalPoints.toInt()} pts"
                        "Batting" -> "${p.runsScored} runs (SR ${p.strikeRate.toInt()})"
                        "Bowling" -> "${p.wickets} wkts (Econ ${String.format(Locale.US, "%.1f", p.economy)})"
                        "All-Rounders" -> "${p.runsScored} runs / ${p.wickets} wkts"
                        "Fielding" -> "${p.catches} C | ${p.directRunOuts} RO | ${p.stumpings} St"
                        else -> "${p.totalPoints.toInt()} pts"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
                        border = BorderStroke(0.5.dp, MpsMediumGray)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Simple index/rank display
                                Text(
                                    text = "#${sortedPlayers.indexOf(p) + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = MpsGold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.width(36.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 14.sp)
                                    Text("${p.teamName} • ${p.role}", color = MpsLightGray, fontSize = 11.sp)
                                }
                            }
                            Text(statText, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicTeamsScreen(
    viewModel: MpsViewModel,
    teams: List<TeamEntity>,
    players: List<PlayerEntity>
) {
    var selectedTeam by remember { mutableStateOf<TeamEntity?>(null) }

    if (selectedTeam != null) {
        val teamPlayers = players.filter { it.teamId == selectedTeam!!.id }
        Column(modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedTeam = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MpsGold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedTeam!!.name.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsWhite)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
                border = BorderStroke(1.dp, MpsGold)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TEAM PROFILE & STATS", fontWeight = FontWeight.Bold, color = MpsGold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Captain: ${selectedTeam!!.captain.ifBlank { "N/A" }}", color = MpsWhite, fontSize = 14.sp)
                    Text("Matches Played: ${selectedTeam!!.matches}", color = MpsWhite, fontSize = 14.sp)
                    Text("Wins: ${selectedTeam!!.wins}  |  Losses: ${selectedTeam!!.losses}", color = MpsWhite, fontSize = 14.sp)
                    Text("Points: ${selectedTeam!!.teamPoints}  |  NRR: ${selectedTeam!!.netRunRate}", color = MpsWhite, fontSize = 14.sp)
                }
            }

            Text("PLAYERS SQUAD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MpsGold)
            Spacer(modifier = Modifier.height(8.dp))

            if (teamPlayers.isEmpty()) {
                Text("No players registered in this team.", color = MpsLightGray, modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(teamPlayers) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MpsMediumGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 14.sp)
                                    Text(p.role, color = MpsLightGray, fontSize = 11.sp)
                                }
                                Text("${p.totalPoints.toInt()} pts", fontWeight = FontWeight.Bold, color = MpsGold)
                            }
                        }
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("🛡️ TEAM RANKINGS & STANDINGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
            Spacer(modifier = Modifier.height(12.dp))

            if (teams.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No teams registered yet.", color = MpsLightGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(teams.sortedByDescending { team -> team.teamPoints }) { team ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTeam = team },
                            colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
                            border = BorderStroke(0.5.dp, MpsMediumGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Groups, contentDescription = null, tint = MpsGold, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(team.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 16.sp)
                                        Text("Captain: ${team.captain.ifBlank { "N/A" }}", color = MpsLightGray, fontSize = 12.sp)
                                        Text("Wins: ${team.wins} | Losses: ${team.losses} | NRR: ${team.netRunRate}", color = MpsLightGray, fontSize = 11.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${team.teamPoints} PTS", fontWeight = FontWeight.ExtraBold, color = MpsGold, fontSize = 16.sp)
                                    Text("Rank #${teams.indexOf(team) + 1}", color = MpsLightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicSearchScreen(
    viewModel: MpsViewModel,
    players: List<PlayerEntity>,
    teams: List<TeamEntity>,
    matches: List<MatchEntity>
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchCategory by remember { mutableStateOf("Players") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("🔍 UNIFIED TOURNAMENT SEARCH", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
        Spacer(modifier = Modifier.height(12.dp))

        // Category Tab Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Players", "Teams", "Matches").forEach { category ->
                MpsFilterChip(
                    selected = searchCategory == category,
                    onClick = { searchCategory = category },
                    label = category
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, team, location...", color = MpsLightGray) },
            modifier = Modifier.fillMaxWidth().testTag("universal_search_field"),
            colors = mpsTextFieldColors(),
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MpsGold) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Display results based on category
        when (searchCategory) {
            "Players" -> {
                val filteredPlayers = players.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.teamName.contains(searchQuery, ignoreCase = true)
                }
                if (filteredPlayers.isEmpty()) {
                    Text("No players found.", color = MpsLightGray, modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredPlayers) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(p.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 15.sp)
                                            Text("${p.teamName} • ${p.role}", color = MpsLightGray, fontSize = 12.sp)
                                        }
                                        Text("${p.totalPoints.toInt()} PTS", fontWeight = FontWeight.Bold, color = MpsGold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Runs: ${p.runsScored} (SR ${p.strikeRate.toInt()})", color = MpsWhite, fontSize = 12.sp)
                                        Text("Wickets: ${p.wickets} (Econ ${String.format(Locale.US, "%.1f", p.economy)})", color = MpsWhite, fontSize = 12.sp)
                                        Text("Catches: ${p.catches}", color = MpsWhite, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "Teams" -> {
                val filteredTeams = teams.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.captain.contains(searchQuery, ignoreCase = true)
                }
                if (filteredTeams.isEmpty()) {
                    Text("No teams found.", color = MpsLightGray, modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredTeams) { team ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(team.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 16.sp)
                                        Text("Captain: ${team.captain}", color = MpsLightGray, fontSize = 12.sp)
                                    }
                                    Text("${team.teamPoints} PTS", fontWeight = FontWeight.Bold, color = MpsGold)
                                }
                            }
                        }
                    }
                }
            }
            "Matches" -> {
                val filteredMatches = matches.filter {
                    it.tournament.contains(searchQuery, ignoreCase = true) ||
                    it.venue.contains(searchQuery, ignoreCase = true) ||
                    it.teamAName.contains(searchQuery, ignoreCase = true) ||
                    it.teamBName.contains(searchQuery, ignoreCase = true)
                }
                if (filteredMatches.isEmpty()) {
                    Text("No matches found.", color = MpsLightGray, modifier = Modifier.padding(8.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredMatches) { m ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(m.tournament.uppercase(), color = MpsGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(m.teamAName, fontWeight = FontWeight.Bold, color = MpsWhite)
                                            Text(m.scoreA.ifBlank { "N/A" }, color = MpsLightGray, fontSize = 12.sp)
                                        }
                                        Text("VS", color = MpsGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(m.teamBName, fontWeight = FontWeight.Bold, color = MpsWhite)
                                            Text(m.scoreB.ifBlank { "N/A" }, color = MpsLightGray, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MpsMediumGray)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Venue: ${m.venue} • Date: ${m.date}", color = MpsLightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicAboutScreen(config: PointSystemConfigEntity) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("ℹ️ ABOUT & SYSTEM RULES", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Mabola Point System (MPS) is a high-performance cricket tournament analyzer that automatically computes player career progression and team standouts based on individual match statistics.",
                    color = MpsWhite,
                    fontSize = 13.sp
                )
            }
        }

        Text("📋 AUTOMATIC POINT SYSTEM RULES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MpsGold)

        // Point rules layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RuleItem("Batting Run", "${config.runPoint} Pt")
                RuleItem("Boundary Four Bonus", "+${config.fourBonus} Pt")
                RuleItem("Boundary Six Bonus", "+${config.sixBonus} Pt")
                RuleItem("30 Runs Bonus", "+${config.runs30Bonus} Pts")
                RuleItem("50 Runs Half-Century Bonus", "+${config.runs50Bonus} Pts")
                RuleItem("100 Runs Century Bonus", "+${config.runs100Bonus} Pts")
                RuleItem("Strike Rate > 150 Bonus (Min ${config.strikeRateMinBalls} balls)", "+${config.strikeRateBonus} Pts")
                RuleItem("Duck Penalty", "${config.duckPenalty} Pts")
                Divider(color = MpsMediumGray)
                RuleItem("Wicket Point", "${config.wicketPoint} Pts")
                RuleItem("Maiden Over Bonus", "+${config.maidenBonus} Pts")
                RuleItem("3 Wicket Haul", "+${config.wickets3Bonus} Pts")
                RuleItem("5 Wicket Haul", "+${config.wickets5Bonus} Pts")
                RuleItem("Economy < 5.0 Bonus", "+${config.economyBonus} Pts")
                Divider(color = MpsMediumGray)
                RuleItem("Outfield Catch", "+${config.catchPoint} Pts")
                RuleItem("Direct Run Out", "+${config.directRunOutPoint} Pts")
                RuleItem("Run Out Assist", "+${config.runOutAssistPoint} Pts")
                RuleItem("Stumping", "+${config.stumpingPoint} Pts")
                Divider(color = MpsMediumGray)
                RuleItem("Player of the Match (POM)", "+${config.playerOfMatchBonus} Pts")
                RuleItem("Winner Team Member", "+${config.winBonus} Pts")
            }
        }
    }
}

@Composable
fun RuleItem(label: String, pts: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MpsWhite, fontSize = 13.sp)
        Text(pts, color = MpsGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun AdminLoginScreen(viewModel: MpsViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
            border = BorderStroke(1.dp, MpsGold),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin Lock",
                    tint = MpsGold,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    "ADMIN SECURE PORTAL",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MpsWhite
                )

                if (error != null) {
                    Text(error!!, color = MpsRed, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Admin Email", color = MpsLightGray) },
                    modifier = Modifier.fillMaxWidth().testTag("admin_email_field"),
                    singleLine = true,
                    colors = mpsTextFieldColors()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = MpsLightGray) },
                    modifier = Modifier.fillMaxWidth().testTag("admin_password_field"),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = mpsTextFieldColors()
                )

                Button(
                    onClick = {
                        viewModel.login(email, password) { success ->
                            if (success) {
                                Toast.makeText(context, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                                viewModel.navigateAdminTo("Dashboard")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_login_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ACCESS DASHBOARD", color = MpsBlack, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { viewModel.navigateTo("Home") }) {
                    Text("Return to Public View", color = MpsGold)
                }
            }
        }
    }
}

// ==========================================
// ADMIN DASHBOARD & MANAGEMENT SCREENS
// ==========================================

@Composable
fun AdminDashboardScreen(
    viewModel: MpsViewModel,
    players: List<PlayerEntity>,
    teams: List<TeamEntity>,
    matches: List<MatchEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("📊 ADMIN CONSOLE OVERVIEW", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatSumCard(Modifier.weight(1f), "TEAMS", teams.size.toString(), Icons.Default.Groups)
                StatSumCard(Modifier.weight(1f), "PLAYERS", players.size.toString(), Icons.Default.Person)
                StatSumCard(Modifier.weight(1f), "MATCHES", matches.size.toString(), Icons.Default.SportsCricket)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("QUICK PANEL ACTIONS", fontWeight = FontWeight.Bold, color = MpsGold)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateAdminTo("Players") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray)
                        ) {
                            Text("Add Player", color = MpsWhite, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.navigateAdminTo("Teams") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray)
                        ) {
                            Text("Add Team", color = MpsWhite, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.navigateAdminTo("Matches") },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray)
                        ) {
                            Text("Add Match", color = MpsWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPlayersScreen(
    viewModel: MpsViewModel,
    players: List<PlayerEntity>,
    teams: List<TeamEntity>
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var selectedTeamIndex by remember { mutableStateOf(0) }
    var role by remember { mutableStateOf("Batsman") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👤 PLAYER REGISTRATION", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
            Button(
                onClick = {
                    if (teams.isEmpty()) {
                        name = ""
                    } else {
                        showDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MpsGold),
                modifier = Modifier.testTag("add_player_dialog_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Player", color = MpsBlack)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (teams.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
            ) {
                Text(
                    "⚠️ PLEASE CREATE A TEAM FIRST BEFORE ADDING PLAYERS.",
                    color = MpsRed,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(players) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(p.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 14.sp)
                            Text("${p.teamName} • ${p.role}", color = MpsLightGray, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { viewModel.deletePlayer(p) },
                            modifier = Modifier.testTag("delete_player_${p.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MpsRed)
                        }
                    }
                }
            }
        }
    }

    if (showDialog && teams.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("REGISTER NEW PLAYER", color = MpsWhite) },
            containerColor = MpsDarkGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Player Name", color = MpsLightGray) },
                        modifier = Modifier.fillMaxWidth().testTag("player_name_field"),
                        colors = mpsTextFieldColors()
                    )

                    // Team Picker
                    Text("Select Team", color = MpsGold, fontSize = 12.sp)
                    teams.forEachIndexed { idx, team ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTeamIndex = idx }
                                .background(if (selectedTeamIndex == idx) MpsMediumGray else Color.Transparent)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedTeamIndex == idx, onClick = { selectedTeamIndex = idx })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(team.name, color = MpsWhite)
                        }
                    }

                    // Role Picker
                    Text("Select Role", color = MpsGold, fontSize = 12.sp)
                    listOf("Batsman", "Bowler", "All-Rounder", "Wicket-Keeper").forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { role = r }
                                .background(if (role == r) MpsMediumGray else Color.Transparent)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = role == r, onClick = { role = r })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(r, color = MpsWhite)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val selectedTeam = teams[selectedTeamIndex]
                            viewModel.addPlayer(name, selectedTeam.id, selectedTeam.name, role)
                            showDialog = false
                            name = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold)
                ) {
                    Text("Register", color = MpsBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MpsWhite)
                }
            }
        )
    }
}

@Composable
fun AdminTeamsScreen(
    viewModel: MpsViewModel,
    teams: List<TeamEntity>
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛡️ TEAM MANAGEMENT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MpsGold),
                modifier = Modifier.testTag("add_team_dialog_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Team", color = MpsBlack)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(teams) { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(team.name, fontWeight = FontWeight.Bold, color = MpsWhite, fontSize = 15.sp)
                            Text("Captain: ${team.captain.ifBlank { "N/A" }}", color = MpsLightGray, fontSize = 12.sp)
                        }
                        IconButton(
                            onClick = { viewModel.deleteTeam(team) },
                            modifier = Modifier.testTag("delete_team_${team.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MpsRed)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("ADD NEW TEAM", color = MpsWhite) },
            containerColor = MpsDarkGray,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Team Name", color = MpsLightGray) },
                        modifier = Modifier.fillMaxWidth().testTag("team_name_field"),
                        colors = mpsTextFieldColors()
                    )

                    OutlinedTextField(
                        value = captain,
                        onValueChange = { captain = it },
                        label = { Text("Captain Name", color = MpsLightGray) },
                        modifier = Modifier.fillMaxWidth().testTag("team_captain_field"),
                        colors = mpsTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addTeam(name, captain)
                            showDialog = false
                            name = ""
                            captain = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold)
                ) {
                    Text("Add Team", color = MpsBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = MpsWhite)
                }
            }
        )
    }
}

@Composable
fun AdminMatchesScreen(
    viewModel: MpsViewModel,
    matches: List<MatchEntity>,
    teams: List<TeamEntity>,
    players: List<PlayerEntity>
) {
    var showAddMatchForm by remember { mutableStateOf(false) }

    // Match form fields
    var tournament by remember { mutableStateOf("Mabola Premier League 2026") }
    var venue by remember { mutableStateOf("Mabola Oval Arena") }
    var date by remember { mutableStateOf("2026-07-03") }
    var time by remember { mutableStateOf("14:00") }
    
    var teamAIndex by remember { mutableStateOf(0) }
    var teamBIndex by remember { mutableStateOf(1) }
    var winnerIndex by remember { mutableStateOf(0) } // 0 = Team A, 1 = Team B, 2 = Draw

    var scoreA by remember { mutableStateOf("") }
    var scoreB by remember { mutableStateOf("") }

    // Temporary match player stats maps
    // Key: playerId, Value: Performance model
    val performancesMap = remember { mutableStateMapOf<Int, PlayerMatchPerformanceEntity>() }

    if (showAddMatchForm) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showAddMatchForm = false }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MpsGold)
                }
                Text("RECORD MATCH & SCORECARD", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsWhite)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (teams.size < 2) {
                Card(colors = CardDefaults.cardColors(containerColor = MpsDarkGray)) {
                    Text(
                        "⚠️ CANNOT CREATE MATCH: YOU NEED AT LEAST 2 TEAMS CREATED FIRST.",
                        color = MpsRed,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    border = BorderStroke(1.dp, MpsGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("MATCH BASICS", fontWeight = FontWeight.Bold, color = MpsGold)

                        OutlinedTextField(
                            value = tournament,
                            onValueChange = { tournament = it },
                            label = { Text("Tournament / Cup", color = MpsLightGray) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = mpsTextFieldColors()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = venue,
                                onValueChange = { venue = it },
                                label = { Text("Venue Location", color = MpsLightGray) },
                                modifier = Modifier.weight(1f),
                                colors = mpsTextFieldColors()
                            )
                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Date (YYYY-MM-DD)", color = MpsLightGray) },
                                modifier = Modifier.weight(1f),
                                colors = mpsTextFieldColors()
                            )
                        }

                        // Team pickers
                        Text("Pick Opponents", color = MpsGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Team A
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Team A", color = MpsWhite, fontSize = 11.sp)
                                teams.forEachIndexed { index, team ->
                                    if (index != teamBIndex) {
                                        Row(
                                            modifier = Modifier.clickable { teamAIndex = index },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = teamAIndex == index, onClick = { teamAIndex = index })
                                            Text(team.name, color = MpsWhite, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            // Team B
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text("Team B", color = MpsWhite, fontSize = 11.sp)
                                teams.forEachIndexed { index, team ->
                                    if (index != teamAIndex) {
                                        Row(
                                            modifier = Modifier.clickable { teamBIndex = index },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = teamBIndex == index, onClick = { teamBIndex = index })
                                            Text(team.name, color = MpsWhite, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // Scorecards
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = scoreA,
                                onValueChange = { scoreA = it },
                                label = { Text("Score Team A (e.g. 150/5)", color = MpsLightGray) },
                                modifier = Modifier.weight(1f),
                                colors = mpsTextFieldColors()
                            )
                            OutlinedTextField(
                                value = scoreB,
                                onValueChange = { scoreB = it },
                                label = { Text("Score Team B (e.g. 148/8)", color = MpsLightGray) },
                                modifier = Modifier.weight(1f),
                                colors = mpsTextFieldColors()
                            )
                        }

                        // Winner picker
                        Text("Select Match Winner", color = MpsGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = winnerIndex == 0, onClick = { winnerIndex = 0 })
                                Text("Team A Winner", color = MpsWhite, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = winnerIndex == 1, onClick = { winnerIndex = 1 })
                                Text("Team B Winner", color = MpsWhite, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = winnerIndex == 2, onClick = { winnerIndex = 2 })
                                Text("Draw/No Winner", color = MpsWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Dynamic Player performance scorecard entry
                val teamA = teams[teamAIndex]
                val teamB = teams[teamBIndex]
                val eligiblePlayers = players.filter { it.teamId == teamA.id || it.teamId == teamB.id }

                Text("🏏 INDIVIDUAL SCORECARD PERFORMANCES", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MpsGold)
                Spacer(modifier = Modifier.height(8.dp))

                eligiblePlayers.forEach { player ->
                    // Initialize performance in map if not present
                    if (!performancesMap.containsKey(player.id)) {
                        performancesMap[player.id] = PlayerMatchPerformanceEntity(
                            matchId = 0,
                            playerId = player.id,
                            playerName = player.name,
                            teamId = player.teamId,
                            teamName = player.teamName,
                            isWinner = if (winnerIndex == 0 && player.teamId == teamA.id) true else if (winnerIndex == 1 && player.teamId == teamB.id) true else false
                        )
                    }

                    val currentPerf = performancesMap[player.id]!!

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${player.name} (${player.teamName})", fontWeight = FontWeight.Bold, color = MpsGold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Stats Inputs Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = if (currentPerf.runs == 0) "" else currentPerf.runs.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(runs = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Runs", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.balls == 0) "" else currentPerf.balls.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(balls = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Balls", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.fours == 0) "" else currentPerf.fours.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(fours = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("4s", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.sixes == 0) "" else currentPerf.sixes.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(sixes = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("6s", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Bowling stats row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = if (currentPerf.wickets == 0) "" else currentPerf.wickets.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(wickets = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Wkts", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.oversBowled == 0.0) "" else currentPerf.oversBowled.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(oversBowled = it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("Overs", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.runsConceded == 0) "" else currentPerf.runsConceded.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(runsConceded = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("RC", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.maidens == 0) "" else currentPerf.maidens.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(maidens = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Mdns", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Fielding stats row & buttons
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = if (currentPerf.catches == 0) "" else currentPerf.catches.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(catches = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Ctch", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.stumpings == 0) "" else currentPerf.stumpings.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(stumpings = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Stmp", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = if (currentPerf.additionalPoints == 0) "" else currentPerf.additionalPoints.toString(),
                                    onValueChange = {
                                        performancesMap[player.id] = currentPerf.copy(additionalPoints = it.toIntOrNull() ?: 0)
                                    },
                                    label = { Text("Add Pt", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = mpsTextFieldColors()
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Player Of Match Selection
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = currentPerf.isPlayerOfMatch,
                                    onCheckedChange = { isChecked ->
                                        // Clear POM from all others first
                                        if (isChecked) {
                                            performancesMap.forEach { (pid, pf) ->
                                                performancesMap[pid] = pf.copy(isPlayerOfMatch = false)
                                            }
                                        }
                                        performancesMap[player.id] = currentPerf.copy(isPlayerOfMatch = isChecked)
                                    }
                                )
                                Text("Player of Match (POM)", color = MpsWhite, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val finalWinnerId = if (winnerIndex == 0) teamA.id else if (winnerIndex == 1) teamB.id else 0
                        
                        // Sync winner values
                        val match = MatchEntity(
                            tournament = tournament,
                            venue = venue,
                            date = date,
                            time = time,
                            teamAId = teamA.id,
                            teamBId = teamB.id,
                            teamAName = teamA.name,
                            teamBName = teamB.name,
                            winnerTeamId = finalWinnerId,
                            scoreA = scoreA,
                            scoreB = scoreB
                        )

                        // Convert scorecard map to clean list
                        val listPerfs = performancesMap.values.map { pf ->
                            val isWinnerPlayer = if (winnerIndex == 0 && pf.teamId == teamA.id) true else if (winnerIndex == 1 && pf.teamId == teamB.id) true else false
                            pf.copy(isWinner = isWinnerPlayer)
                        }

                        viewModel.addMatch(match, listPerfs)
                        showAddMatchForm = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_match_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold)
                ) {
                    Text("SAVE MATCH SCORECARD & UPDATE LEADERBOARDS", color = MpsBlack, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏏 MATCH LISTINGS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)
                Button(
                    onClick = { showAddMatchForm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold),
                    modifier = Modifier.testTag("record_match_view_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Record Match", color = MpsBlack)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (matches.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No match records added.", color = MpsLightGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matches) { m ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MpsDarkGray)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.tournament.uppercase(), color = MpsGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { viewModel.deleteMatch(m) },
                                        modifier = Modifier.testTag("delete_match_${m.id}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MpsRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.teamAName, fontWeight = FontWeight.Bold, color = MpsWhite)
                                        Text(m.scoreA.ifBlank { "N/A" }, color = MpsLightGray, fontSize = 12.sp)
                                    }
                                    Text("VS", color = MpsGold, fontWeight = FontWeight.Bold)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(m.teamBName, fontWeight = FontWeight.Bold, color = MpsWhite)
                                        Text(m.scoreB.ifBlank { "N/A" }, color = MpsLightGray, fontSize = 12.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = MpsMediumGray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Venue: ${m.venue}  |  Date: ${m.date}", color = MpsLightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSettingsScreen(
    viewModel: MpsViewModel,
    pointConfig: PointSystemConfigEntity,
    adminConfig: AdminConfigEntity
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backupString by remember { mutableStateOf("") }
    var restoreString by remember { mutableStateOf("") }

    // Multipliers values state
    var runsMultiplier by remember(pointConfig) { mutableStateOf(pointConfig.runPoint.toString()) }
    var fourMultiplier by remember(pointConfig) { mutableStateOf(pointConfig.fourBonus.toString()) }
    var sixMultiplier by remember(pointConfig) { mutableStateOf(pointConfig.sixBonus.toString()) }
    var wktMultiplier by remember(pointConfig) { mutableStateOf(pointConfig.wicketPoint.toString()) }
    var catchMultiplier by remember(pointConfig) { mutableStateOf(pointConfig.catchPoint.toString()) }

    // Admin state
    var emailInput by remember(adminConfig) { mutableStateOf(adminConfig.adminEmail) }
    var passwordInput by remember(adminConfig) { mutableStateOf(adminConfig.adminPassword) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("⚙️ SETTINGS & RULES CONFIGURATION", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MpsGold)

        // Point Customizer Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, MpsMediumGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("1. CUSTOMIZE POINT SYSTEM MULTIPLIERS", fontWeight = FontWeight.Bold, color = MpsGold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = runsMultiplier,
                        onValueChange = { runsMultiplier = it },
                        label = { Text("Points per Run") },
                        modifier = Modifier.weight(1f),
                        colors = mpsTextFieldColors()
                    )
                    OutlinedTextField(
                        value = fourMultiplier,
                        onValueChange = { fourMultiplier = it },
                        label = { Text("Four Boundary Bonus") },
                        modifier = Modifier.weight(1f),
                        colors = mpsTextFieldColors()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sixMultiplier,
                        onValueChange = { sixMultiplier = it },
                        label = { Text("Six Boundary Bonus") },
                        modifier = Modifier.weight(1f),
                        colors = mpsTextFieldColors()
                    )
                    OutlinedTextField(
                        value = wktMultiplier,
                        onValueChange = { wktMultiplier = it },
                        label = { Text("Points per Wicket") },
                        modifier = Modifier.weight(1f),
                        colors = mpsTextFieldColors()
                    )
                }

                OutlinedTextField(
                    value = catchMultiplier,
                    onValueChange = { catchMultiplier = it },
                    label = { Text("Points per Outfield Catch") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mpsTextFieldColors()
                )

                Button(
                    onClick = {
                        val runMultiplierVal = runsMultiplier.toDoubleOrNull() ?: 1.0
                        val fourMultiplierVal = fourMultiplier.toDoubleOrNull() ?: 1.0
                        val sixMultiplierVal = sixMultiplier.toDoubleOrNull() ?: 2.0
                        val wktMultiplierVal = wktMultiplier.toDoubleOrNull() ?: 25.0
                        val catchMultiplierVal = catchMultiplier.toDoubleOrNull() ?: 8.0

                        viewModel.updatePointSystem(pointConfig.copy(
                            runPoint = runMultiplierVal,
                            fourBonus = fourMultiplierVal,
                            sixBonus = sixMultiplierVal,
                            wicketPoint = wktMultiplierVal,
                            catchPoint = catchMultiplierVal
                        ))
                        Toast.makeText(context, "Point System rules successfully updated!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold)
                ) {
                    Text("SAVE RULES & RE-CALCULATE STANDINGS", color = MpsBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Admin config card
        Card(
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, MpsMediumGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("2. CHANGE ADMIN SECURE CREDENTIALS", fontWeight = FontWeight.Bold, color = MpsGold)

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Admin secure Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mpsTextFieldColors()
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Admin secure Password") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mpsTextFieldColors()
                )

                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                            viewModel.updateAdminCredentials(emailInput, passwordInput)
                            Toast.makeText(context, "Credentials saved successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsGold)
                ) {
                    Text("SAVE NEW CREDENTIALS", color = MpsBlack, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Database utilities Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MpsDarkGray),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(0.5.dp, MpsMediumGray)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("3. DATABASE BACKUP & RESTORE UTILITIES", fontWeight = FontWeight.Bold, color = MpsGold)

                Button(
                    onClick = {
                        coroutineScope.launch {
                            backupString = viewModel.getBackupData()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsMediumGray)
                ) {
                    Text("GENERATE BACKUP ENCODED TEXT", color = MpsWhite)
                }

                if (backupString.isNotBlank()) {
                    OutlinedTextField(
                        value = backupString,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Backup Output (Copy this safely)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = mpsTextFieldColors()
                    )
                }

                Divider(color = MpsMediumGray)

                OutlinedTextField(
                    value = restoreString,
                    onValueChange = { restoreString = it },
                    label = { Text("Paste Backup Code here to Restore") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mpsTextFieldColors()
                )

                Button(
                    onClick = {
                        if (restoreString.isNotBlank()) {
                            viewModel.restoreDatabase(restoreString,
                                onSuccess = {
                                    Toast.makeText(context, "Database successfully restored!", Toast.LENGTH_LONG).show()
                                    restoreString = ""
                                },
                                onFailure = {
                                    Toast.makeText(context, "Invalid backup string format", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MpsRed)
                ) {
                    Text("EXECUTE RESTORE & OVERWRITE DATABASE", color = MpsWhite, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
