package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MpsDao {
    // Players
    @Query("SELECT * FROM players ORDER BY totalPoints DESC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    // Teams
    @Query("SELECT * FROM teams ORDER BY teamPoints DESC, netRunRate DESC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id LIMIT 1")
    suspend fun getTeamById(id: Int): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    @Query("DELETE FROM teams")
    suspend fun deleteAllTeams()

    // Matches
    @Query("SELECT * FROM matches ORDER BY date DESC, time DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches")
    suspend fun deleteAllMatches()

    // Player Match Performances
    @Query("SELECT * FROM player_match_performances")
    fun getAllPerformances(): Flow<List<PlayerMatchPerformanceEntity>>

    @Query("SELECT * FROM player_match_performances WHERE matchId = :matchId")
    fun getPerformancesForMatch(matchId: Int): Flow<List<PlayerMatchPerformanceEntity>>

    @Query("SELECT * FROM player_match_performances WHERE playerId = :playerId")
    fun getPerformancesForPlayer(playerId: Int): Flow<List<PlayerMatchPerformanceEntity>>

    @Query("SELECT * FROM player_match_performances WHERE playerId = :playerId")
    suspend fun getPerformancesForPlayerList(playerId: Int): List<PlayerMatchPerformanceEntity>

    @Query("SELECT * FROM player_match_performances WHERE matchId = :matchId")
    suspend fun getPerformancesForMatchList(matchId: Int): List<PlayerMatchPerformanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(perf: PlayerMatchPerformanceEntity): Long

    @Query("DELETE FROM player_match_performances WHERE matchId = :matchId")
    suspend fun deletePerformancesByMatch(matchId: Int)

    @Query("DELETE FROM player_match_performances WHERE id = :id")
    suspend fun deletePerformanceById(id: Int)

    @Query("DELETE FROM player_match_performances")
    suspend fun deleteAllPerformances()

    // Configs (Point System)
    @Query("SELECT * FROM point_system_configs WHERE id = 1 LIMIT 1")
    fun getPointSystemConfigFlow(): Flow<PointSystemConfigEntity?>

    @Query("SELECT * FROM point_system_configs WHERE id = 1 LIMIT 1")
    suspend fun getPointSystemConfig(): PointSystemConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPointSystemConfig(config: PointSystemConfigEntity)

    // Configs (Admin Config)
    @Query("SELECT * FROM admin_configs WHERE id = 1 LIMIT 1")
    fun getAdminConfigFlow(): Flow<AdminConfigEntity?>

    @Query("SELECT * FROM admin_configs WHERE id = 1 LIMIT 1")
    suspend fun getAdminConfig(): AdminConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminConfig(config: AdminConfigEntity)
}
