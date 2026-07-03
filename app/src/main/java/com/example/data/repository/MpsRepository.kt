package com.example.data.repository

import android.util.Log
import com.example.data.dao.MpsDao
import com.example.data.model.*
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class MpsRepository(private val mpsDao: MpsDao) {

    val allPlayers: Flow<List<PlayerEntity>> = mpsDao.getAllPlayers()
    val allTeams: Flow<List<TeamEntity>> = mpsDao.getAllTeams()
    val allMatches: Flow<List<MatchEntity>> = mpsDao.getAllMatches()
    val allPerformances: Flow<List<PlayerMatchPerformanceEntity>> = mpsDao.getAllPerformances()
    val pointSystemConfigFlow: Flow<PointSystemConfigEntity?> = mpsDao.getPointSystemConfigFlow()
    val adminConfigFlow: Flow<AdminConfigEntity?> = mpsDao.getAdminConfigFlow()

    suspend fun syncWithFirestore() {
        if (!FirebaseManager.isFirebaseEnabled) return
        val db = FirebaseManager.db ?: return
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch configs
                val configTask = db.collection("configs").document("point_system").get()
                val configDoc = Tasks.await(configTask, 5, java.util.concurrent.TimeUnit.SECONDS)
                if (configDoc.exists()) {
                    val data = configDoc.data
                    if (data != null) {
                        val config = FirestoreSync.run { data.toPointSystemConfigEntity() }
                        mpsDao.insertPointSystemConfig(config)
                    }
                }

                // 2. Fetch Teams
                val teamsTask = db.collection("teams").get()
                val teamsSnap = Tasks.await(teamsTask, 5, java.util.concurrent.TimeUnit.SECONDS)
                val teamsList = teamsSnap.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreSync.run { it.toTeamEntity() } }
                }

                // 3. Fetch Players
                val playersTask = db.collection("players").get()
                val playersSnap = Tasks.await(playersTask, 5, java.util.concurrent.TimeUnit.SECONDS)
                val playersList = playersSnap.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreSync.run { it.toPlayerEntity() } }
                }

                // 4. Fetch Matches
                val matchesTask = db.collection("matches").get()
                val matchesSnap = Tasks.await(matchesTask, 5, java.util.concurrent.TimeUnit.SECONDS)
                val matchesList = matchesSnap.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreSync.run { it.toMatchEntity() } }
                }

                // 5. Fetch Performances
                val perfsTask = db.collection("performances").get()
                val perfsSnap = Tasks.await(perfsTask, 5, java.util.concurrent.TimeUnit.SECONDS)
                val perfsList = perfsSnap.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreSync.run { it.toPlayerMatchPerformanceEntity() } }
                }

                // Only overwrite if we retrieved actual data from Firestore
                if (teamsList.isNotEmpty() || playersList.isNotEmpty() || matchesList.isNotEmpty()) {
                    Log.i("MpsRepository", "Syncing from Firestore: found ${teamsList.size} teams, ${playersList.size} players, ${matchesList.size} matches.")
                    // Clear existing
                    mpsDao.deleteAllPlayers()
                    mpsDao.deleteAllTeams()
                    mpsDao.deleteAllMatches()
                    mpsDao.deleteAllPerformances()

                    // Insert downloaded
                    teamsList.forEach { mpsDao.insertTeam(it) }
                    playersList.forEach { mpsDao.insertPlayer(it) }
                    matchesList.forEach { mpsDao.insertMatch(it) }
                    perfsList.forEach { mpsDao.insertPerformance(it) }
                } else {
                    Log.i("MpsRepository", "Firestore database is empty. No data to pull.")
                }
            } catch (e: Exception) {
                Log.e("MpsRepository", "Error pulling data from Firestore: ${e.message}", e)
            }
        }
    }

    // Initialize default configs if they do not exist
    suspend fun initializeConfigs() {
        withContext(Dispatchers.IO) {
            val psConfig = mpsDao.getPointSystemConfig()
            if (psConfig == null) {
                mpsDao.insertPointSystemConfig(PointSystemConfigEntity())
            }
            val adminConfig = mpsDao.getAdminConfig()
            if (adminConfig == null) {
                mpsDao.insertAdminConfig(AdminConfigEntity())
            }
        }
    }

    // Backup Database (Generates a raw string format representing all data)
    suspend fun backupDatabase(): String {
        return withContext(Dispatchers.IO) {
            val players = mpsDao.getAllPlayers().firstOrNull() ?: emptyList()
            val teams = mpsDao.getAllTeams().firstOrNull() ?: emptyList()
            val matches = mpsDao.getAllMatches().firstOrNull() ?: emptyList()
            val perfs = mpsDao.getAllPerformances().firstOrNull() ?: emptyList()
            val psConfig = mpsDao.getPointSystemConfig() ?: PointSystemConfigEntity()
            val adminConfig = mpsDao.getAdminConfig() ?: AdminConfigEntity()

            val sb = java.lang.StringBuilder()
            sb.append("MPS_BACKUP_V1\n")
            
            // Admin Config
            sb.append("ADMIN_CONFIG|${adminConfig.adminEmail}|${adminConfig.adminPassword}|${adminConfig.appLogoUrl}\n")
            
            // Point System Config
            sb.append("POINT_SYSTEM|${psConfig.runPoint}|${psConfig.fourBonus}|${psConfig.sixBonus}|${psConfig.runs30Bonus}|${psConfig.runs50Bonus}|${psConfig.runs100Bonus}|${psConfig.strikeRateBonus}|${psConfig.strikeRateThreshold}|${psConfig.strikeRateMinBalls}|${psConfig.duckPenalty}|${psConfig.wicketPoint}|${psConfig.maidenBonus}|${psConfig.wickets3Bonus}|${psConfig.wickets5Bonus}|${psConfig.economyBonus}|${psConfig.economyThreshold}|${psConfig.catchPoint}|${psConfig.directRunOutPoint}|${psConfig.runOutAssistPoint}|${psConfig.stumpingPoint}|${psConfig.playerOfMatchBonus}|${psConfig.winBonus}\n")
            
            // Teams
            sb.append("TEAMS_COUNT|${teams.size}\n")
            teams.forEach { t ->
                sb.append("TEAM|${t.id}|${t.name}|${t.captain}|${t.logoUrl}|${t.matches}|${t.wins}|${t.losses}|${t.netRunRate}|${t.teamPoints}|${t.ranking}\n")
            }

            // Players
            sb.append("PLAYERS_COUNT|${players.size}\n")
            players.forEach { p ->
                sb.append("PLAYER|${p.id}|${p.name}|${p.teamId}|${p.teamName}|${p.role}|${p.photoUrl}\n")
            }

            // Matches
            sb.append("MATCHES_COUNT|${matches.size}\n")
            matches.forEach { m ->
                sb.append("MATCH|${m.id}|${m.tournament}|${m.venue}|${m.date}|${m.time}|${m.teamAId}|${m.teamBId}|${m.teamAName}|${m.teamBName}|${m.winnerTeamId}|${m.scoreA}|${m.scoreB}\n")
            }

            // Performances
            sb.append("PERFORMANCES_COUNT|${perfs.size}\n")
            perfs.forEach { pf ->
                sb.append("PERFORMANCE|${pf.id}|${pf.matchId}|${pf.playerId}|${pf.playerName}|${pf.teamId}|${pf.teamName}|${pf.runs}|${pf.balls}|${pf.fours}|${pf.sixes}|${pf.wickets}|${pf.oversBowled}|${pf.runsConceded}|${pf.maidens}|${pf.catches}|${pf.directRunOuts}|${pf.runOutAssists}|${pf.stumpings}|${pf.isPlayerOfMatch}|${pf.isWinner}|${pf.additionalPoints}|${pf.totalPoints}\n")
            }

            sb.toString()
        }
    }

    // Restore Database from String
    suspend fun restoreDatabase(backupStr: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!backupStr.startsWith("MPS_BACKUP_V1")) return@withContext false
                
                // Clear existing
                mpsDao.deleteAllPlayers()
                mpsDao.deleteAllTeams()
                mpsDao.deleteAllMatches()
                mpsDao.deleteAllPerformances()

                val lines = backupStr.split("\n")
                lines.forEach { line ->
                    if (line.isBlank()) return@forEach
                    val parts = line.split("|")
                    when (parts[0]) {
                        "ADMIN_CONFIG" -> {
                            if (parts.size >= 4) {
                                mpsDao.insertAdminConfig(AdminConfigEntity(adminEmail = parts[1], adminPassword = parts[2], appLogoUrl = parts[3]))
                            }
                        }
                        "POINT_SYSTEM" -> {
                            if (parts.size >= 23) {
                                val cfg = PointSystemConfigEntity(
                                    runPoint = parts[1].toDoubleOrNull() ?: 1.0,
                                    fourBonus = parts[2].toDoubleOrNull() ?: 1.0,
                                    sixBonus = parts[3].toDoubleOrNull() ?: 2.0,
                                    runs30Bonus = parts[4].toDoubleOrNull() ?: 5.0,
                                    runs50Bonus = parts[5].toDoubleOrNull() ?: 10.0,
                                    runs100Bonus = parts[6].toDoubleOrNull() ?: 20.0,
                                    strikeRateBonus = parts[7].toDoubleOrNull() ?: 5.0,
                                    strikeRateThreshold = parts[8].toDoubleOrNull() ?: 150.0,
                                    strikeRateMinBalls = parts[9].toIntOrNull() ?: 10,
                                    duckPenalty = parts[10].toDoubleOrNull() ?: -5.0,
                                    wicketPoint = parts[11].toDoubleOrNull() ?: 25.0,
                                    maidenBonus = parts[12].toDoubleOrNull() ?: 10.0,
                                    wickets3Bonus = parts[13].toDoubleOrNull() ?: 10.0,
                                    wickets5Bonus = parts[14].toDoubleOrNull() ?: 20.0,
                                    economyBonus = parts[15].toDoubleOrNull() ?: 10.0,
                                    economyThreshold = parts[16].toDoubleOrNull() ?: 5.0,
                                    catchPoint = parts[17].toDoubleOrNull() ?: 8.0,
                                    directRunOutPoint = parts[18].toDoubleOrNull() ?: 12.0,
                                    runOutAssistPoint = parts[19].toDoubleOrNull() ?: 6.0,
                                    stumpingPoint = parts[20].toDoubleOrNull() ?: 12.0,
                                    playerOfMatchBonus = parts[21].toDoubleOrNull() ?: 30.0,
                                    winBonus = parts[22].toDoubleOrNull() ?: 5.0
                                )
                                mpsDao.insertPointSystemConfig(cfg)
                            }
                        }
                        "TEAM" -> {
                            if (parts.size >= 11) {
                                val t = TeamEntity(
                                    id = parts[1].toIntOrNull() ?: 0,
                                    name = parts[2],
                                    captain = parts[3],
                                    logoUrl = parts[4],
                                    matches = parts[5].toIntOrNull() ?: 0,
                                    wins = parts[6].toIntOrNull() ?: 0,
                                    losses = parts[7].toIntOrNull() ?: 0,
                                    netRunRate = parts[8].toDoubleOrNull() ?: 0.0,
                                    teamPoints = parts[9].toIntOrNull() ?: 0,
                                    ranking = parts[10].toIntOrNull() ?: 0
                                )
                                mpsDao.insertTeam(t)
                            }
                        }
                        "PLAYER" -> {
                            if (parts.size >= 7) {
                                val p = PlayerEntity(
                                    id = parts[1].toIntOrNull() ?: 0,
                                    name = parts[2],
                                    teamId = parts[3].toIntOrNull() ?: 0,
                                    teamName = parts[4],
                                    role = parts[5],
                                    photoUrl = parts[6]
                                )
                                mpsDao.insertPlayer(p)
                            }
                        }
                        "MATCH" -> {
                            if (parts.size >= 13) {
                                val m = MatchEntity(
                                    id = parts[1].toIntOrNull() ?: 0,
                                    tournament = parts[2],
                                    venue = parts[3],
                                    date = parts[4],
                                    time = parts[5],
                                    teamAId = parts[6].toIntOrNull() ?: 0,
                                    teamBId = parts[7].toIntOrNull() ?: 0,
                                    teamAName = parts[8],
                                    teamBName = parts[9],
                                    winnerTeamId = parts[10].toIntOrNull() ?: 0,
                                    scoreA = parts[11],
                                    scoreB = parts[12]
                                )
                                mpsDao.insertMatch(m)
                            }
                        }
                        "PERFORMANCE" -> {
                            if (parts.size >= 23) {
                                val pf = PlayerMatchPerformanceEntity(
                                    id = parts[1].toIntOrNull() ?: 0,
                                    matchId = parts[2].toIntOrNull() ?: 0,
                                    playerId = parts[3].toIntOrNull() ?: 0,
                                    playerName = parts[4],
                                    teamId = parts[5].toIntOrNull() ?: 0,
                                    teamName = parts[6],
                                    runs = parts[7].toIntOrNull() ?: 0,
                                    balls = parts[8].toIntOrNull() ?: 0,
                                    fours = parts[9].toIntOrNull() ?: 0,
                                    sixes = parts[10].toIntOrNull() ?: 0,
                                    wickets = parts[11].toIntOrNull() ?: 0,
                                    oversBowled = parts[12].toDoubleOrNull() ?: 0.0,
                                    runsConceded = parts[13].toIntOrNull() ?: 0,
                                    maidens = parts[14].toIntOrNull() ?: 0,
                                    catches = parts[15].toIntOrNull() ?: 0,
                                    directRunOuts = parts[16].toIntOrNull() ?: 0,
                                    runOutAssists = parts[17].toIntOrNull() ?: 0,
                                    stumpings = parts[18].toIntOrNull() ?: 0,
                                    isPlayerOfMatch = parts[19].toBooleanStrictOrNull() ?: false,
                                    isWinner = parts[20].toBooleanStrictOrNull() ?: false,
                                    additionalPoints = parts[21].toIntOrNull() ?: 0,
                                    totalPoints = parts[22].toDoubleOrNull() ?: 0.0
                                )
                                mpsDao.insertPerformance(pf)
                            }
                        }
                    }
                }
                
                // Recalculate all players and teams
                recalculateAll()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    // Players
    suspend fun insertPlayer(player: PlayerEntity) {
        withContext(Dispatchers.IO) {
            val generatedId = mpsDao.insertPlayer(player).toInt()
            val finalPlayer = player.copy(id = generatedId)
            FirestoreSync.savePlayer(finalPlayer)
        }
    }
    suspend fun updatePlayer(player: PlayerEntity) {
        withContext(Dispatchers.IO) {
            mpsDao.updatePlayer(player)
            FirestoreSync.savePlayer(player)
        }
    }
    suspend fun deletePlayer(player: PlayerEntity) {
        withContext(Dispatchers.IO) {
            mpsDao.deletePlayer(player)
            FirestoreSync.deletePlayer(player.id)
        }
    }
    suspend fun getPlayerById(id: Int) = mpsDao.getPlayerById(id)

    // Teams
    suspend fun insertTeam(team: TeamEntity) {
        withContext(Dispatchers.IO) {
            val generatedId = mpsDao.insertTeam(team).toInt()
            val finalTeam = team.copy(id = generatedId)
            FirestoreSync.saveTeam(finalTeam)
        }
    }
    suspend fun updateTeam(team: TeamEntity) {
        withContext(Dispatchers.IO) {
            mpsDao.updateTeam(team)
            FirestoreSync.saveTeam(team)
        }
    }
    suspend fun deleteTeam(team: TeamEntity) {
        withContext(Dispatchers.IO) {
            mpsDao.deleteTeam(team)
            FirestoreSync.deleteTeam(team.id)
        }
    }
    suspend fun getTeamById(id: Int) = mpsDao.getTeamById(id)

    // Point Config
    suspend fun savePointSystemConfig(config: PointSystemConfigEntity) {
        withContext(Dispatchers.IO) {
            mpsDao.insertPointSystemConfig(config)
            recalculateAll()
            FirestoreSync.savePointSystemConfig(config)
            // Sync updated players career points to Firestore too
            val allPlayersList = mpsDao.getAllPlayers().firstOrNull() ?: emptyList()
            allPlayersList.forEach { p -> FirestoreSync.savePlayer(p) }
        }
    }

    // Admin Config
    suspend fun saveAdminConfig(config: AdminConfigEntity) {
        mpsDao.insertAdminConfig(config)
    }

    // Matches
    suspend fun insertMatch(match: MatchEntity, performances: List<PlayerMatchPerformanceEntity>) {
        withContext(Dispatchers.IO) {
            val matchId = mpsDao.insertMatch(match).toInt()
            
            // Delete any existing performances for this match (if it was an edit/re-save)
            mpsDao.deletePerformancesByMatch(matchId)
            
            // Insert performances with correct matchId
            val pointConfig = mpsDao.getPointSystemConfig() ?: PointSystemConfigEntity()
            val finalPerfs = performances.map { perf ->
                val calculatedPoints = calculatePerformancePoints(perf, pointConfig)
                val tempPerf = perf.copy(matchId = matchId, totalPoints = calculatedPoints)
                val generatedPerfId = mpsDao.insertPerformance(tempPerf).toInt()
                tempPerf.copy(id = generatedPerfId)
            }
            
            // Recalculate stats for players involved and teams involved
            recalculateAll()

            // Sync updated match, performances, players, and teams to Firestore
            val updatedMatch = match.copy(id = matchId)
            FirestoreSync.saveMatch(updatedMatch, finalPerfs)
            
            val allPlayersList = mpsDao.getAllPlayers().firstOrNull() ?: emptyList()
            allPlayersList.forEach { p -> FirestoreSync.savePlayer(p) }
            
            val allTeamsList = mpsDao.getAllTeams().firstOrNull() ?: emptyList()
            allTeamsList.forEach { t -> FirestoreSync.saveTeam(t) }
        }
    }

    suspend fun deleteMatch(match: MatchEntity) {
        withContext(Dispatchers.IO) {
            val associatedPerfs = mpsDao.getAllPerformances().firstOrNull()?.filter { it.matchId == match.id } ?: emptyList()
            
            mpsDao.deleteMatch(match)
            mpsDao.deletePerformancesByMatch(match.id)
            recalculateAll()
            
            // Delete from Firestore
            FirestoreSync.deleteMatch(match.id, associatedPerfs)
            
            // Sync updated players and teams to Firestore
            val allPlayersList = mpsDao.getAllPlayers().firstOrNull() ?: emptyList()
            allPlayersList.forEach { p -> FirestoreSync.savePlayer(p) }
            
            val allTeamsList = mpsDao.getAllTeams().firstOrNull() ?: emptyList()
            allTeamsList.forEach { t -> FirestoreSync.saveTeam(t) }
        }
    }

    // Calculations & Relational Syncing
    suspend fun recalculateAll() {
        val pointConfig = mpsDao.getPointSystemConfig() ?: PointSystemConfigEntity()
        val allPlayersList = mpsDao.getAllPlayers().firstOrNull() ?: return
        val allTeamsList = mpsDao.getAllTeams().firstOrNull() ?: return
        val allMatchesList = mpsDao.getAllMatches().firstOrNull() ?: return

        // 1. Recalculate every individual performance point
        val allPerfs = mpsDao.getAllPerformances().firstOrNull() ?: emptyList()
        allPerfs.forEach { perf ->
            val updatedPoints = calculatePerformancePoints(perf, pointConfig)
            if (updatedPoints != perf.totalPoints) {
                mpsDao.insertPerformance(perf.copy(totalPoints = updatedPoints))
            }
        }

        // 2. Re-aggregate Career Stats for each Player
        allPlayersList.forEach { player ->
            val playerPerfs = mpsDao.getPerformancesForPlayerList(player.id)
            if (playerPerfs.isEmpty()) {
                // reset career stats
                mpsDao.updatePlayer(player.copy(
                    matchesPlayed = 0, runsScored = 0, ballsFaced = 0, fours = 0, sixes = 0, strikeRate = 0.0,
                    wickets = 0, oversBowled = 0.0, maidens = 0, runsConceded = 0, economy = 0.0, catches = 0,
                    directRunOuts = 0, runOutAssists = 0, stumpings = 0, playerOfMatchCount = 0, winsCount = 0,
                    additionalPoints = 0, totalPoints = 0.0
                ))
            } else {
                val matchesPlayed = playerPerfs.size
                val runsScored = playerPerfs.sumOf { it.runs }
                val ballsFaced = playerPerfs.sumOf { it.balls }
                val fours = playerPerfs.sumOf { it.fours }
                val sixes = playerPerfs.sumOf { it.sixes }
                val strikeRate = if (ballsFaced > 0) (runsScored.toDouble() / ballsFaced) * 100.0 else 0.0
                val wickets = playerPerfs.sumOf { it.wickets }
                val maidens = playerPerfs.sumOf { it.maidens }
                val runsConceded = playerPerfs.sumOf { it.runsConceded }
                
                // Sum overs correctly
                var totalBallsBowled = 0
                playerPerfs.forEach { pf ->
                    val wholeOvers = pf.oversBowled.toInt()
                    val fraction = ((pf.oversBowled - wholeOvers) * 10).roundToInt()
                    totalBallsBowled += (wholeOvers * 6) + fraction
                }
                val finalOversBowled = (totalBallsBowled / 6) + ((totalBallsBowled % 6) / 10.0)
                val economy = if (totalBallsBowled > 0) (runsConceded.toDouble() / (totalBallsBowled.toDouble() / 6.0)) else 0.0

                val catches = playerPerfs.sumOf { it.catches }
                val directRunOuts = playerPerfs.sumOf { it.directRunOuts }
                val runOutAssists = playerPerfs.sumOf { it.runOutAssists }
                val stumpings = playerPerfs.sumOf { it.stumpings }
                val pomCount = playerPerfs.count { it.isPlayerOfMatch }
                val winsCount = playerPerfs.count { pf -> pf.isWinner }
                val additionalPoints = playerPerfs.sumOf { it.additionalPoints }
                val totalPoints = playerPerfs.sumOf { it.totalPoints }

                mpsDao.updatePlayer(player.copy(
                    matchesPlayed = matchesPlayed,
                    runsScored = runsScored,
                    ballsFaced = ballsFaced,
                    fours = fours,
                    sixes = sixes,
                    strikeRate = strikeRate,
                    wickets = wickets,
                    oversBowled = finalOversBowled,
                    maidens = maidens,
                    runsConceded = runsConceded,
                    economy = economy,
                    catches = catches,
                    directRunOuts = directRunOuts,
                    runOutAssists = runOutAssists,
                    stumpings = stumpings,
                    playerOfMatchCount = pomCount,
                    winsCount = winsCount,
                    additionalPoints = additionalPoints,
                    totalPoints = totalPoints
                ))
            }
        }

        // 3. Re-aggregate Stats for each Team
        allTeamsList.forEach { team ->
            val teamMatches = allMatchesList.filter { m -> m.teamAId == team.id || m.teamBId == team.id }
            val wins = teamMatches.count { m -> m.winnerTeamId == team.id }
            val losses = teamMatches.count { m -> m.winnerTeamId != 0 && m.winnerTeamId != team.id }
            val played = teamMatches.size
            val teamPoints = wins * 2 // 2 points per win

            // NRR calculation: can be kept as is or editable, let's keep the existing NRR unless it is recalculated
            // We can update the Team record
            mpsDao.updateTeam(team.copy(
                matches = played,
                wins = wins,
                losses = losses,
                teamPoints = teamPoints
            ))
        }
    }

    private fun calculatePerformancePoints(perf: PlayerMatchPerformanceEntity, config: PointSystemConfigEntity): Double {
        var pts = 0.0
        
        // Batting
        pts += perf.runs * config.runPoint
        pts += perf.fours * config.fourBonus
        pts += perf.sixes * config.sixBonus
        
        if (perf.runs >= 100) {
            pts += config.runs100Bonus
        } else if (perf.runs >= 50) {
            pts += config.runs50Bonus
        } else if (perf.runs >= 30) {
            pts += config.runs30Bonus
        }
        
        if (perf.balls >= config.strikeRateMinBalls) {
            val sr = (perf.runs.toDouble() / perf.balls.toDouble()) * 100.0
            if (sr >= config.strikeRateThreshold) {
                pts += config.strikeRateBonus
            }
        }
        
        if (perf.runs == 0 && perf.balls > 0) {
            pts += config.duckPenalty
        }
        
        // Bowling
        pts += perf.wickets * config.wicketPoint
        pts += perf.maidens * config.maidenBonus
        
        if (perf.wickets >= 5) {
            pts += config.wickets5Bonus
        } else if (perf.wickets >= 3) {
            pts += config.wickets3Bonus
        }
        
        if (perf.oversBowled > 0.0) {
            val wholeOvers = perf.oversBowled.toInt()
            val fraction = ((perf.oversBowled - wholeOvers) * 10).roundToInt()
            val totalBalls = (wholeOvers * 6) + fraction
            if (totalBalls > 0) {
                val econ = perf.runsConceded.toDouble() / (totalBalls.toDouble() / 6.0)
                if (econ < config.economyThreshold) {
                    pts += config.economyBonus
                }
            }
        }
        
        // Fielding
        pts += perf.catches * config.catchPoint
        pts += perf.directRunOuts * config.directRunOutPoint
        pts += perf.runOutAssists * config.runOutAssistPoint
        pts += perf.stumpings * config.stumpingPoint
        
        // Awards
        if (perf.isPlayerOfMatch) {
            pts += config.playerOfMatchBonus
        }
        if (perf.isWinner) {
            pts += config.winBonus
        }
        
        // Additional Points (Admin manual override)
        pts += perf.additionalPoints
        
        return pts
    }
}
