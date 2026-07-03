package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

object FirestoreSync {
    private const val TAG = "FirestoreSync"

    // --- MAPPER EXTENSIONS ---

    fun PlayerEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "teamId" to teamId,
        "teamName" to teamName,
        "role" to role,
        "photoUrl" to photoUrl,
        "matchesPlayed" to matchesPlayed,
        "runsScored" to runsScored,
        "ballsFaced" to ballsFaced,
        "fours" to fours,
        "sixes" to sixes,
        "strikeRate" to strikeRate,
        "wickets" to wickets,
        "oversBowled" to oversBowled,
        "maidens" to maidens,
        "runsConceded" to runsConceded,
        "economy" to economy,
        "catches" to catches,
        "directRunOuts" to directRunOuts,
        "runOutAssists" to runOutAssists,
        "stumpings" to stumpings,
        "playerOfMatchCount" to playerOfMatchCount,
        "winsCount" to winsCount,
        "additionalPoints" to additionalPoints,
        "totalPoints" to totalPoints
    )

    fun Map<String, Any>.toPlayerEntity(): PlayerEntity = PlayerEntity(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        name = this["name"] as? String ?: "",
        teamId = (this["teamId"] as? Number)?.toInt() ?: 0,
        teamName = this["teamName"] as? String ?: "",
        role = this["role"] as? String ?: "",
        photoUrl = this["photoUrl"] as? String ?: "",
        matchesPlayed = (this["matchesPlayed"] as? Number)?.toInt() ?: 0,
        runsScored = (this["runsScored"] as? Number)?.toInt() ?: 0,
        ballsFaced = (this["ballsFaced"] as? Number)?.toInt() ?: 0,
        fours = (this["fours"] as? Number)?.toInt() ?: 0,
        sixes = (this["sixes"] as? Number)?.toInt() ?: 0,
        strikeRate = (this["strikeRate"] as? Number)?.toDouble() ?: 0.0,
        wickets = (this["wickets"] as? Number)?.toInt() ?: 0,
        oversBowled = (this["oversBowled"] as? Number)?.toDouble() ?: 0.0,
        maidens = (this["maidens"] as? Number)?.toInt() ?: 0,
        runsConceded = (this["runsConceded"] as? Number)?.toInt() ?: 0,
        economy = (this["economy"] as? Number)?.toDouble() ?: 0.0,
        catches = (this["catches"] as? Number)?.toInt() ?: 0,
        directRunOuts = (this["directRunOuts"] as? Number)?.toInt() ?: 0,
        runOutAssists = (this["runOutAssists"] as? Number)?.toInt() ?: 0,
        stumpings = (this["stumpings"] as? Number)?.toInt() ?: 0,
        playerOfMatchCount = (this["playerOfMatchCount"] as? Number)?.toInt() ?: 0,
        winsCount = (this["winsCount"] as? Number)?.toInt() ?: 0,
        additionalPoints = (this["additionalPoints"] as? Number)?.toInt() ?: 0,
        totalPoints = (this["totalPoints"] as? Number)?.toDouble() ?: 0.0
    )

    fun TeamEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "captain" to captain,
        "logoUrl" to logoUrl,
        "matches" to matches,
        "wins" to wins,
        "losses" to losses,
        "netRunRate" to netRunRate,
        "teamPoints" to teamPoints,
        "ranking" to ranking
    )

    fun Map<String, Any>.toTeamEntity(): TeamEntity = TeamEntity(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        name = this["name"] as? String ?: "",
        captain = this["captain"] as? String ?: "",
        logoUrl = this["logoUrl"] as? String ?: "",
        matches = (this["matches"] as? Number)?.toInt() ?: 0,
        wins = (this["wins"] as? Number)?.toInt() ?: 0,
        losses = (this["losses"] as? Number)?.toInt() ?: 0,
        netRunRate = (this["netRunRate"] as? Number)?.toDouble() ?: 0.0,
        teamPoints = (this["teamPoints"] as? Number)?.toInt() ?: 0,
        ranking = (this["ranking"] as? Number)?.toInt() ?: 0
    )

    fun MatchEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "tournament" to tournament,
        "venue" to venue,
        "date" to date,
        "time" to time,
        "teamAId" to teamAId,
        "teamBId" to teamBId,
        "teamAName" to teamAName,
        "teamBName" to teamBName,
        "winnerTeamId" to winnerTeamId,
        "scoreA" to scoreA,
        "scoreB" to scoreB
    )

    fun Map<String, Any>.toMatchEntity(): MatchEntity = MatchEntity(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        tournament = this["tournament"] as? String ?: "",
        venue = this["venue"] as? String ?: "",
        date = this["date"] as? String ?: "",
        time = this["time"] as? String ?: "",
        teamAId = (this["teamAId"] as? Number)?.toInt() ?: 0,
        teamBId = (this["teamBId"] as? Number)?.toInt() ?: 0,
        teamAName = this["teamAName"] as? String ?: "",
        teamBName = this["teamBName"] as? String ?: "",
        winnerTeamId = (this["winnerTeamId"] as? Number)?.toInt() ?: 0,
        scoreA = this["scoreA"] as? String ?: "",
        scoreB = this["scoreB"] as? String ?: ""
    )

    fun PlayerMatchPerformanceEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "matchId" to matchId,
        "playerId" to playerId,
        "playerName" to playerName,
        "teamId" to teamId,
        "teamName" to teamName,
        "runs" to runs,
        "balls" to balls,
        "fours" to fours,
        "sixes" to sixes,
        "wickets" to wickets,
        "oversBowled" to oversBowled,
        "runsConceded" to runsConceded,
        "maidens" to maidens,
        "catches" to catches,
        "directRunOuts" to directRunOuts,
        "runOutAssists" to runOutAssists,
        "stumpings" to stumpings,
        "isPlayerOfMatch" to isPlayerOfMatch,
        "isWinner" to isWinner,
        "additionalPoints" to additionalPoints,
        "totalPoints" to totalPoints
    )

    fun Map<String, Any>.toPlayerMatchPerformanceEntity(): PlayerMatchPerformanceEntity = PlayerMatchPerformanceEntity(
        id = (this["id"] as? Number)?.toInt() ?: 0,
        matchId = (this["matchId"] as? Number)?.toInt() ?: 0,
        playerId = (this["playerId"] as? Number)?.toInt() ?: 0,
        playerName = this["playerName"] as? String ?: "",
        teamId = (this["teamId"] as? Number)?.toInt() ?: 0,
        teamName = this["teamName"] as? String ?: "",
        runs = (this["runs"] as? Number)?.toInt() ?: 0,
        balls = (this["balls"] as? Number)?.toInt() ?: 0,
        fours = (this["fours"] as? Number)?.toInt() ?: 0,
        sixes = (this["sixes"] as? Number)?.toInt() ?: 0,
        wickets = (this["wickets"] as? Number)?.toInt() ?: 0,
        oversBowled = (this["oversBowled"] as? Number)?.toDouble() ?: 0.0,
        runsConceded = (this["runsConceded"] as? Number)?.toInt() ?: 0,
        maidens = (this["maidens"] as? Number)?.toInt() ?: 0,
        catches = (this["catches"] as? Number)?.toInt() ?: 0,
        directRunOuts = (this["directRunOuts"] as? Number)?.toInt() ?: 0,
        runOutAssists = (this["runOutAssists"] as? Number)?.toInt() ?: 0,
        stumpings = (this["stumpings"] as? Number)?.toInt() ?: 0,
        isPlayerOfMatch = this["isPlayerOfMatch"] as? Boolean ?: false,
        isWinner = this["isWinner"] as? Boolean ?: false,
        additionalPoints = (this["additionalPoints"] as? Number)?.toInt() ?: 0,
        totalPoints = (this["totalPoints"] as? Number)?.toDouble() ?: 0.0
    )

    fun PointSystemConfigEntity.toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "runPoint" to runPoint,
        "fourBonus" to fourBonus,
        "sixBonus" to sixBonus,
        "runs30Bonus" to runs30Bonus,
        "runs50Bonus" to runs50Bonus,
        "runs100Bonus" to runs100Bonus,
        "strikeRateBonus" to strikeRateBonus,
        "strikeRateThreshold" to strikeRateThreshold,
        "strikeRateMinBalls" to strikeRateMinBalls,
        "duckPenalty" to duckPenalty,
        "wicketPoint" to wicketPoint,
        "maidenBonus" to maidenBonus,
        "wickets3Bonus" to wickets3Bonus,
        "wickets5Bonus" to wickets5Bonus,
        "economyBonus" to economyBonus,
        "economyThreshold" to economyThreshold,
        "catchPoint" to catchPoint,
        "directRunOutPoint" to directRunOutPoint,
        "runOutAssistPoint" to runOutAssistPoint,
        "stumpingPoint" to stumpingPoint,
        "playerOfMatchBonus" to playerOfMatchBonus,
        "winBonus" to winBonus
    )

    fun Map<String, Any>.toPointSystemConfigEntity(): PointSystemConfigEntity = PointSystemConfigEntity(
        id = (this["id"] as? Number)?.toInt() ?: 1,
        runPoint = (this["runPoint"] as? Number)?.toDouble() ?: 1.0,
        fourBonus = (this["fourBonus"] as? Number)?.toDouble() ?: 1.0,
        sixBonus = (this["sixBonus"] as? Number)?.toDouble() ?: 2.0,
        runs30Bonus = (this["runs30Bonus"] as? Number)?.toDouble() ?: 5.0,
        runs50Bonus = (this["runs50Bonus"] as? Number)?.toDouble() ?: 10.0,
        runs100Bonus = (this["runs100Bonus"] as? Number)?.toDouble() ?: 20.0,
        strikeRateBonus = (this["strikeRateBonus"] as? Number)?.toDouble() ?: 5.0,
        strikeRateThreshold = (this["strikeRateThreshold"] as? Number)?.toDouble() ?: 150.0,
        strikeRateMinBalls = (this["strikeRateMinBalls"] as? Number)?.toInt() ?: 10,
        duckPenalty = (this["duckPenalty"] as? Number)?.toDouble() ?: -5.0,
        wicketPoint = (this["wicketPoint"] as? Number)?.toDouble() ?: 25.0,
        maidenBonus = (this["maidenBonus"] as? Number)?.toDouble() ?: 10.0,
        wickets3Bonus = (this["wickets3Bonus"] as? Number)?.toDouble() ?: 10.0,
        wickets5Bonus = (this["wickets5Bonus"] as? Number)?.toDouble() ?: 20.0,
        economyBonus = (this["economyBonus"] as? Number)?.toDouble() ?: 10.0,
        economyThreshold = (this["economyThreshold"] as? Number)?.toDouble() ?: 5.0,
        catchPoint = (this["catchPoint"] as? Number)?.toDouble() ?: 8.0,
        directRunOutPoint = (this["directRunOutPoint"] as? Number)?.toDouble() ?: 12.0,
        runOutAssistPoint = (this["runOutAssistPoint"] as? Number)?.toDouble() ?: 6.0,
        stumpingPoint = (this["stumpingPoint"] as? Number)?.toDouble() ?: 12.0,
        playerOfMatchBonus = (this["playerOfMatchBonus"] as? Number)?.toDouble() ?: 30.0,
        winBonus = (this["winBonus"] as? Number)?.toDouble() ?: 5.0
    )

    // --- FIRESTORE DATABASE WRITES ---

    fun savePlayer(player: PlayerEntity) {
        val db = FirebaseManager.db ?: return
        try {
            val task = db.collection("players")
                .document(player.id.toString())
                .set(player.toMap())
            Tasks.await(task, 5, TimeUnit.SECONDS)
            Log.d(TAG, "Player successfully saved to Firestore: ${player.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving player to Firestore: ${e.message}")
        }
    }

    fun deletePlayer(playerId: Int) {
        val db = FirebaseManager.db ?: return
        try {
            val task = db.collection("players")
                .document(playerId.toString())
                .delete()
            Tasks.await(task, 5, TimeUnit.SECONDS)
            Log.d(TAG, "Player successfully deleted from Firestore: $playerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting player from Firestore: ${e.message}")
        }
    }

    fun saveTeam(team: TeamEntity) {
        val db = FirebaseManager.db ?: return
        try {
            val task = db.collection("teams")
                .document(team.id.toString())
                .set(team.toMap())
            Tasks.await(task, 5, TimeUnit.SECONDS)
            Log.d(TAG, "Team successfully saved to Firestore: ${team.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving team to Firestore: ${e.message}")
        }
    }

    fun deleteTeam(teamId: Int) {
        val db = FirebaseManager.db ?: return
        try {
            val task = db.collection("teams")
                .document(teamId.toString())
                .delete()
            Tasks.await(task, 5, TimeUnit.SECONDS)
            Log.d(TAG, "Team successfully deleted from Firestore: $teamId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting team from Firestore: ${e.message}")
        }
    }

    fun saveMatch(match: MatchEntity, performances: List<PlayerMatchPerformanceEntity>) {
        val db = FirebaseManager.db ?: return
        try {
            // Save match entity
            val matchTask = db.collection("matches")
                .document(match.id.toString())
                .set(match.toMap())
            Tasks.await(matchTask, 5, TimeUnit.SECONDS)

            // Save performances
            performances.forEach { perf ->
                val perfTask = db.collection("performances")
                    .document(perf.id.toString())
                    .set(perf.toMap())
                Tasks.await(perfTask, 5, TimeUnit.SECONDS)
            }
            Log.d(TAG, "Match and performances saved to Firestore: ${match.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving match/performances to Firestore: ${e.message}")
        }
    }

    fun deleteMatch(matchId: Int, performances: List<PlayerMatchPerformanceEntity>) {
        val db = FirebaseManager.db ?: return
        try {
            // Delete match
            val matchTask = db.collection("matches")
                .document(matchId.toString())
                .delete()
            Tasks.await(matchTask, 5, TimeUnit.SECONDS)

            // Delete associated performances
            performances.forEach { perf ->
                val perfTask = db.collection("performances")
                    .document(perf.id.toString())
                    .delete()
                Tasks.await(perfTask, 5, TimeUnit.SECONDS)
            }
            Log.d(TAG, "Match and performances deleted from Firestore: $matchId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting match from Firestore: ${e.message}")
        }
    }

    fun savePointSystemConfig(config: PointSystemConfigEntity) {
        val db = FirebaseManager.db ?: return
        try {
            val task = db.collection("configs")
                .document("point_system")
                .set(config.toMap())
            Tasks.await(task, 5, TimeUnit.SECONDS)
            Log.d(TAG, "PointSystemConfig saved to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving PointSystemConfig to Firestore: ${e.message}")
        }
    }
}
