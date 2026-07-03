package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.MpsDatabase
import com.example.data.model.*
import com.example.data.repository.FirebaseManager
import com.example.data.repository.MpsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MpsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MpsRepository
    
    // Core State Flows from Room
    val players: StateFlow<List<PlayerEntity>>
    val teams: StateFlow<List<TeamEntity>>
    val matches: StateFlow<List<MatchEntity>>
    val pointSystemConfig: StateFlow<PointSystemConfigEntity?>
    val adminConfig: StateFlow<AdminConfigEntity?>

    // Admin Auth State
    private val _loggedInAdmin = MutableStateFlow<AdminConfigEntity?>(null)
    val loggedInAdmin: StateFlow<AdminConfigEntity?> = _loggedInAdmin.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Public Navigation State
    private val _currentScreen = MutableStateFlow("Home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Admin Navigation State
    private val _currentAdminScreen = MutableStateFlow("Dashboard")
    val currentAdminScreen: StateFlow<String> = _currentAdminScreen.asStateFlow()

    // Search and Filter Queries
    private val _playerSearchQuery = MutableStateFlow("")
    val playerSearchQuery: StateFlow<String> = _playerSearchQuery.asStateFlow()

    private val _teamSearchQuery = MutableStateFlow("")
    val teamSearchQuery: StateFlow<String> = _teamSearchQuery.asStateFlow()

    private val _matchSearchQuery = MutableStateFlow("")
    val matchSearchQuery: StateFlow<String> = _matchSearchQuery.asStateFlow()

    init {
        val database = MpsDatabase.getDatabase(application)
        repository = MpsRepository(database.mpsDao())

        // Launch initial configuration check and Firestore sync
        viewModelScope.launch {
            repository.initializeConfigs()
            repository.syncWithFirestore()
        }

        // Connect streams with StateFlow holders
        players = repository.allPlayers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        teams = repository.allTeams.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        matches = repository.allMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        pointSystemConfig = repository.pointSystemConfigFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PointSystemConfigEntity()
        )

        adminConfig = repository.adminConfigFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminConfigEntity()
        )
    }

    // Public Navigation Actions
    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // Admin Navigation Actions
    fun navigateAdminTo(screen: String) {
        _currentAdminScreen.value = screen
    }

    // Search functions
    fun setPlayerSearchQuery(query: String) {
        _playerSearchQuery.value = query
    }

    fun setTeamSearchQuery(query: String) {
        _teamSearchQuery.value = query
    }

    fun setMatchSearchQuery(query: String) {
        _matchSearchQuery.value = query
    }

    // Authentication
    fun login(email: String, password: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            if (FirebaseManager.isFirebaseEnabled) {
                try {
                    _loginError.value = null
                    val auth = FirebaseManager.auth
                    if (auth != null) {
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val currentAdmin = adminConfig.value ?: AdminConfigEntity()
                                    _loggedInAdmin.value = currentAdmin.copy(adminEmail = email)
                                    _loginError.value = null
                                    onResult(true)
                                } else {
                                    _loginError.value = task.exception?.localizedMessage ?: "Invalid Admin Email or Password"
                                    onResult(false)
                                }
                            }
                    } else {
                        _loginError.value = "Firebase Authentication is not available"
                        onResult(false)
                    }
                } catch (e: Exception) {
                    _loginError.value = e.localizedMessage ?: "Login failed"
                    onResult(false)
                }
            } else {
                val currentAdmin = adminConfig.value ?: AdminConfigEntity()
                if (email.trim().equals(currentAdmin.adminEmail, ignoreCase = true) && 
                    password == currentAdmin.adminPassword) {
                    _loggedInAdmin.value = currentAdmin
                    _loginError.value = null
                    onResult(true)
                } else {
                    _loginError.value = "Invalid Admin Email or Password"
                    onResult(false)
                }
            }
        }
    }

    fun logout() {
        _loggedInAdmin.value = null
    }

    // Player CRUD
    fun addPlayer(name: String, teamId: Int, teamName: String, role: String, photoUrl: String = "") {
        viewModelScope.launch {
            val player = PlayerEntity(
                name = name,
                teamId = teamId,
                teamName = teamName,
                role = role,
                photoUrl = photoUrl
            )
            repository.insertPlayer(player)
        }
    }

    fun updatePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.updatePlayer(player)
        }
    }

    fun deletePlayer(player: PlayerEntity) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    // Team CRUD
    fun addTeam(name: String, captain: String = "", logoUrl: String = "", netRunRate: Double = 0.0) {
        viewModelScope.launch {
            val team = TeamEntity(
                name = name,
                captain = captain,
                logoUrl = logoUrl,
                netRunRate = netRunRate
            )
            repository.insertTeam(team)
        }
    }

    fun updateTeam(team: TeamEntity) {
        viewModelScope.launch {
            repository.updateTeam(team)
        }
    }

    fun deleteTeam(team: TeamEntity) {
        viewModelScope.launch {
            repository.deleteTeam(team)
        }
    }

    // Point System Customization
    fun updatePointSystem(config: PointSystemConfigEntity) {
        viewModelScope.launch {
            repository.savePointSystemConfig(config)
        }
    }

    // Admin Credentials & Logo Settings
    fun updateAdminCredentials(email: String, password: String, logoUrl: String = "") {
        viewModelScope.launch {
            val current = adminConfig.value ?: AdminConfigEntity()
            val updated = current.copy(adminEmail = email, adminPassword = password, appLogoUrl = logoUrl)
            repository.saveAdminConfig(updated)
            // Keep logged in if email/pass was updated successfully
            if (_loggedInAdmin.value != null) {
                _loggedInAdmin.value = updated
            }
        }
    }

    // Match CRUD
    fun addMatch(match: MatchEntity, performances: List<PlayerMatchPerformanceEntity>) {
        viewModelScope.launch {
            repository.insertMatch(match, performances)
        }
    }

    fun deleteMatch(match: MatchEntity) {
        viewModelScope.launch {
            repository.deleteMatch(match)
        }
    }

    // Database Backup & Restore
    suspend fun getBackupData(): String {
        return repository.backupDatabase()
    }

    fun restoreDatabase(backupStr: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreDatabase(backupStr)
            if (success) {
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    // Load Professional Sample Cricket Data for a Premium Visual Experience
    fun loadSampleTournamentData() {
        viewModelScope.launch {
            // First clear all existing records
            val database = MpsDatabase.getDatabase(getApplication())
            val dao = database.mpsDao()
            dao.deleteAllPlayers()
            dao.deleteAllTeams()
            dao.deleteAllMatches()
            dao.deleteAllPerformances()

            // 1. Insert 4 premium Teams
            val teamAId = dao.insertTeam(TeamEntity(name = "Mabola Knights", captain = "Reeza Fajju", logoUrl = "knights")).toInt()
            val teamBId = dao.insertTeam(TeamEntity(name = "Royal Strikers", captain = "Aslam Khan", logoUrl = "strikers")).toInt()
            val teamCId = dao.insertTeam(TeamEntity(name = "Golden Eagles", captain = "Imran Shah", logoUrl = "eagles")).toInt()
            val teamDId = dao.insertTeam(TeamEntity(name = "Elite Panthers", captain = "Yaseen Ally", logoUrl = "panthers")).toInt()

            // 2. Insert Players for Mabola Knights
            val p1 = dao.insertPlayer(PlayerEntity(name = "Reeza Fajju", teamId = teamAId, teamName = "Mabola Knights", role = "All-Rounder")).toInt()
            val p2 = dao.insertPlayer(PlayerEntity(name = "Zaid Patel", teamId = teamAId, teamName = "Mabola Knights", role = "Batsman")).toInt()
            val p3 = dao.insertPlayer(PlayerEntity(name = "Bilal Moosa", teamId = teamAId, teamName = "Mabola Knights", role = "Bowler")).toInt()
            val p4 = dao.insertPlayer(PlayerEntity(name = "Sufyan Coovadia", teamId = teamAId, teamName = "Mabola Knights", role = "Wicket-Keeper")).toInt()

            // Royal Strikers
            val p5 = dao.insertPlayer(PlayerEntity(name = "Aslam Khan", teamId = teamBId, teamName = "Royal Strikers", role = "Batsman")).toInt()
            val p6 = dao.insertPlayer(PlayerEntity(name = "Farhan Bux", teamId = teamBId, teamName = "Royal Strikers", role = "Bowler")).toInt()
            val p7 = dao.insertPlayer(PlayerEntity(name = "Zaheer Dinat", teamId = teamBId, teamName = "Royal Strikers", role = "All-Rounder")).toInt()

            // Golden Eagles
            val p8 = dao.insertPlayer(PlayerEntity(name = "Imran Shah", teamId = teamCId, teamName = "Golden Eagles", role = "Batsman")).toInt()
            val p9 = dao.insertPlayer(PlayerEntity(name = "Aamir Variava", teamId = teamCId, teamName = "Golden Eagles", role = "Bowler")).toInt()
            val p10 = dao.insertPlayer(PlayerEntity(name = "Yusuf Chothia", teamId = teamCId, teamName = "Golden Eagles", role = "All-Rounder")).toInt()

            // Elite Panthers
            val p11 = dao.insertPlayer(PlayerEntity(name = "Yaseen Ally", teamId = teamDId, teamName = "Elite Panthers", role = "Batsman")).toInt()
            val p12 = dao.insertPlayer(PlayerEntity(name = "Shabeer Ahmed", teamId = teamDId, teamName = "Elite Panthers", role = "Bowler")).toInt()
            val p13 = dao.insertPlayer(PlayerEntity(name = "Naeem Dockrat", teamId = teamDId, teamName = "Elite Panthers", role = "All-Rounder")).toInt()

            // 3. Create Point Configuration
            val pConfig = PointSystemConfigEntity()
            dao.insertPointSystemConfig(pConfig)

            // 4. Create 3 Matches with realistic performances
            // Match 1: Mabola Knights vs Royal Strikers
            val m1Id = dao.insertMatch(MatchEntity(
                tournament = "Mabola Premier League 2026",
                venue = "Mabola Oval Arena",
                date = "2026-06-15",
                time = "14:00",
                teamAId = teamAId,
                teamBId = teamBId,
                teamAName = "Mabola Knights",
                teamBName = "Royal Strikers",
                winnerTeamId = teamAId,
                scoreA = "165/4 (20.0)",
                scoreB = "142/8 (20.0)"
            )).toInt()

            // Match 1 Performances
            // Reeza Fajju (Mabola Knights): 52 runs (30 balls), 2 wickets (4.0 overs, 22 runs, 0 maidens), 1 catch, POTM!
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p1, playerName = "Reeza Fajju", teamId = teamAId, teamName = "Mabola Knights",
                runs = 52, balls = 30, fours = 4, sixes = 3, wickets = 2, oversBowled = 4.0, runsConceded = 22, maidens = 0,
                catches = 1, isPlayerOfMatch = true, isWinner = true, additionalPoints = 0
            ))
            // Zaid Patel (Mabola Knights): 45 runs (28 balls), 1 catch
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p2, playerName = "Zaid Patel", teamId = teamAId, teamName = "Mabola Knights",
                runs = 45, balls = 28, fours = 5, sixes = 1, catches = 1, isWinner = true
            ))
            // Bilal Moosa (Mabola Knights): 3 wickets (4.0 overs, 18 runs, 1 maiden)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p3, playerName = "Bilal Moosa", teamId = teamAId, teamName = "Mabola Knights",
                wickets = 3, oversBowled = 4.0, runsConceded = 18, maidens = 1, isWinner = true
            ))
            // Sufyan Coovadia (Mabola Knights): 12 runs (10 balls), 1 catch, 1 stumping
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p4, playerName = "Sufyan Coovadia", teamId = teamAId, teamName = "Mabola Knights",
                runs = 12, balls = 10, catches = 1, stumpings = 1, isWinner = true
            ))
            // Aslam Khan (Royal Strikers): 64 runs (40 balls), 0 wickets, loss
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p5, playerName = "Aslam Khan", teamId = teamBId, teamName = "Royal Strikers",
                runs = 64, balls = 40, fours = 6, sixes = 2, isWinner = false
            ))
            // Farhan Bux (Royal Strikers): 2 wickets (3.0 overs, 28 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p6, playerName = "Farhan Bux", teamId = teamBId, teamName = "Royal Strikers",
                wickets = 2, oversBowled = 3.0, runsConceded = 28, isWinner = false
            ))
            // Zaheer Dinat (Royal Strikers): 18 runs (15 balls), 1 wicket (2.0 overs, 15 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m1Id, playerId = p7, playerName = "Zaheer Dinat", teamId = teamBId, teamName = "Royal Strikers",
                runs = 18, balls = 15, wickets = 1, oversBowled = 2.0, runsConceded = 15, isWinner = false
            ))

            // Match 2: Golden Eagles vs Elite Panthers
            val m2Id = dao.insertMatch(MatchEntity(
                tournament = "Mabola Premier League 2026",
                venue = "Sunset Sports Park",
                date = "2026-06-18",
                time = "15:30",
                teamAId = teamCId,
                teamBId = teamDId,
                teamAName = "Golden Eagles",
                teamBName = "Elite Panthers",
                winnerTeamId = teamCId,
                scoreA = "138/7 (20.0)",
                scoreB = "120/10 (19.2)"
            )).toInt()

            // Match 2 Performances
            // Imran Shah (Golden Eagles): 35 runs (22 balls), isWinner = true
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p8, playerName = "Imran Shah", teamId = teamCId, teamName = "Golden Eagles",
                runs = 35, balls = 22, fours = 3, sixes = 1, isWinner = true
            ))
            // Aamir Variava (Golden Eagles): 5 wickets (3.2 overs, 12 runs, 0 maidens), POTM!
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p9, playerName = "Aamir Variava", teamId = teamCId, teamName = "Golden Eagles",
                wickets = 5, oversBowled = 3.2, runsConceded = 12, isPlayerOfMatch = true, isWinner = true
            ))
            // Yusuf Chothia (Golden Eagles): 15 runs (12 balls), 1 wicket (4.0 overs, 25 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p10, playerName = "Yusuf Chothia", teamId = teamCId, teamName = "Golden Eagles",
                runs = 15, balls = 12, wickets = 1, oversBowled = 4.0, runsConceded = 25, isWinner = true
            ))
            // Yaseen Ally (Elite Panthers): 41 runs (32 balls), isWinner = false
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p11, playerName = "Yaseen Ally", teamId = teamDId, teamName = "Elite Panthers",
                runs = 41, balls = 32, fours = 4, sixes = 0, isWinner = false
            ))
            // Shabeer Ahmed (Elite Panthers): 3 wickets (4.0 overs, 22 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p12, playerName = "Shabeer Ahmed", teamId = teamDId, teamName = "Elite Panthers",
                wickets = 3, oversBowled = 4.0, runsConceded = 22, isWinner = false
            ))
            // Naeem Dockrat (Elite Panthers): 10 runs (9 balls), 1 wicket (3.0 overs, 18 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m2Id, playerId = p13, playerName = "Naeem Dockrat", teamId = teamDId, teamName = "Elite Panthers",
                runs = 10, balls = 9, wickets = 1, oversBowled = 3.0, runsConceded = 18, isWinner = false
            ))

            // Match 3: Mabola Knights vs Golden Eagles
            val m3Id = dao.insertMatch(MatchEntity(
                tournament = "Mabola Premier League 2026",
                venue = "Mabola Oval Arena",
                date = "2026-06-22",
                time = "19:00",
                teamAId = teamAId,
                teamBId = teamCId,
                teamAName = "Mabola Knights",
                teamBName = "Golden Eagles",
                winnerTeamId = teamAId,
                scoreA = "172/3 (20.0)",
                scoreB = "155/6 (20.0)"
            )).toInt()

            // Match 3 Performances
            // Reeza Fajju (Mabola Knights): 48 runs (28 balls), 3 wickets (4.0 overs, 15 runs, 1 maiden), POTM!
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p1, playerName = "Reeza Fajju", teamId = teamAId, teamName = "Mabola Knights",
                runs = 48, balls = 28, fours = 5, sixes = 2, wickets = 3, oversBowled = 4.0, runsConceded = 15, maidens = 1,
                isPlayerOfMatch = true, isWinner = true
            ))
            // Zaid Patel (Mabola Knights): 72 runs (45 balls)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p2, playerName = "Zaid Patel", teamId = teamAId, teamName = "Mabola Knights",
                runs = 72, balls = 45, fours = 8, sixes = 3, isWinner = true
            ))
            // Bilal Moosa (Mabola Knights): 1 wicket (4.0 overs, 32 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p3, playerName = "Bilal Moosa", teamId = teamAId, teamName = "Mabola Knights",
                wickets = 1, oversBowled = 4.0, runsConceded = 32, isWinner = true
            ))
            // Sufyan Coovadia (Mabola Knights): 2 catches
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p4, playerName = "Sufyan Coovadia", teamId = teamAId, teamName = "Mabola Knights",
                catches = 2, isWinner = true
            ))
            // Imran Shah (Golden Eagles): 82 runs (50 balls), loss
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p8, playerName = "Imran Shah", teamId = teamCId, teamName = "Golden Eagles",
                runs = 82, balls = 50, fours = 10, sixes = 4, isWinner = false
            ))
            // Aamir Variava (Golden Eagles): 1 wicket (4.0 overs, 35 runs, 0 maidens)
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p9, playerName = "Aamir Variava", teamId = teamCId, teamName = "Golden Eagles",
                wickets = 1, oversBowled = 4.0, runsConceded = 35, isWinner = false
            ))
            // Yusuf Chothia (Golden Eagles): 22 runs (15 balls), 1 catch
            dao.insertPerformance(PlayerMatchPerformanceEntity(
                matchId = m3Id, playerId = p10, playerName = "Yusuf Chothia", teamId = teamCId, teamName = "Golden Eagles",
                runs = 22, balls = 15, catches = 1, isWinner = false
            ))

            // Trigger complete recalculation to sync all careers and team tables
            repository.recalculateAll()
        }
    }
}
