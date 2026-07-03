package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val teamId: Int,
    val teamName: String,
    val role: String, // e.g. "Batsman", "Bowler", "All-Rounder", "Wicket-Keeper"
    val photoUrl: String = "", // Base64 or empty
    val matchesPlayed: Int = 0,
    val runsScored: Int = 0,
    val ballsFaced: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val strikeRate: Double = 0.0,
    val wickets: Int = 0,
    val oversBowled: Double = 0.0, // represented as 1.5 etc.
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val economy: Double = 0.0,
    val catches: Int = 0,
    val directRunOuts: Int = 0,
    val runOutAssists: Int = 0,
    val stumpings: Int = 0,
    val playerOfMatchCount: Int = 0,
    val winsCount: Int = 0,
    val additionalPoints: Int = 0,
    val totalPoints: Double = 0.0
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val captain: String = "",
    val logoUrl: String = "", // Base64 or empty
    val matches: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val netRunRate: Double = 0.0,
    val teamPoints: Int = 0, // e.g. 2 for win, 0 for loss
    val ranking: Int = 0
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tournament: String,
    val venue: String,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:MM
    val teamAId: Int,
    val teamBId: Int,
    val teamAName: String,
    val teamBName: String,
    val winnerTeamId: Int, // 0 for draw/none, or teamAId/teamBId
    val scoreA: String = "", // e.g., "150/5 (20.0)"
    val scoreB: String = ""  // e.g., "148/8 (20.0)"
)

@Entity(tableName = "player_match_performances")
data class PlayerMatchPerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val playerId: Int,
    val playerName: String = "",
    val teamId: Int = 0,
    val teamName: String = "",
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val wickets: Int = 0,
    val oversBowled: Double = 0.0,
    val runsConceded: Int = 0,
    val maidens: Int = 0,
    val catches: Int = 0,
    val directRunOuts: Int = 0,
    val runOutAssists: Int = 0,
    val stumpings: Int = 0,
    val isPlayerOfMatch: Boolean = false,
    val isWinner: Boolean = false,
    val additionalPoints: Int = 0,
    val totalPoints: Double = 0.0
)

@Entity(tableName = "point_system_configs")
data class PointSystemConfigEntity(
    @PrimaryKey val id: Int = 1,
    val runPoint: Double = 1.0,
    val fourBonus: Double = 1.0,
    val sixBonus: Double = 2.0,
    val runs30Bonus: Double = 5.0,
    val runs50Bonus: Double = 10.0,
    val runs100Bonus: Double = 20.0,
    val strikeRateBonus: Double = 5.0,
    val strikeRateThreshold: Double = 150.0,
    val strikeRateMinBalls: Int = 10,
    val duckPenalty: Double = -5.0,
    val wicketPoint: Double = 25.0,
    val maidenBonus: Double = 10.0,
    val wickets3Bonus: Double = 10.0,
    val wickets5Bonus: Double = 20.0,
    val economyBonus: Double = 10.0,
    val economyThreshold: Double = 5.0,
    val catchPoint: Double = 8.0,
    val directRunOutPoint: Double = 12.0,
    val runOutAssistPoint: Double = 6.0,
    val stumpingPoint: Double = 12.0,
    val playerOfMatchBonus: Double = 30.0,
    val winBonus: Double = 5.0
)

@Entity(tableName = "admin_configs")
data class AdminConfigEntity(
    @PrimaryKey val id: Int = 1,
    val adminEmail: String = "riafajju@gmail.com",
    val adminPassword: String = "Reeza31&",
    val appLogoUrl: String = "" // Base64 custom logo
)
