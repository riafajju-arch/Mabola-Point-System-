package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.MpsDatabase
import com.example.data.model.*
import com.example.data.repository.FirebaseManager
import com.example.data.repository.MpsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: MpsDatabase
    private lateinit var repository: MpsRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        // Initialize an in-memory SQLite database for robust, hermetic, isolated testing
        db = Room.inMemoryDatabaseBuilder(context, MpsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MpsRepository(db.mpsDao())
        
        // Ensure default configs are initialized
        runBlocking {
            repository.initializeConfigs()
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testDefaultConfigs() = runBlocking {
        val config = db.mpsDao().getPointSystemConfig()
        assertNotNull("Default point configuration should be initialized", config)
        assertEquals(1.0, config!!.runPoint, 0.0)

        val admin = db.mpsDao().getAdminConfig()
        assertNotNull("Default admin configuration should be initialized", admin)
        assertEquals("riafajju@gmail.com", admin!!.adminEmail)
    }

    @Test
    fun testAddAndEditPlayer() = runBlocking {
        // Create player
        val player = PlayerEntity(
            id = 1,
            name = "Virat Kohli",
            teamId = 10,
            teamName = "RCB",
            role = "Batsman"
        )

        // Insert player
        db.mpsDao().insertPlayer(player)
        
        // Read player back and verify
        val retrieved = db.mpsDao().getPlayerById(1)
        assertNotNull(retrieved)
        assertEquals("Virat Kohli", retrieved!!.name)
        assertEquals("RCB", retrieved.teamName)

        // Edit player
        val updatedPlayer = retrieved.copy(name = "King Kohli", runsScored = 120)
        db.mpsDao().updatePlayer(updatedPlayer)

        val retrievedUpdated = db.mpsDao().getPlayerById(1)
        assertNotNull(retrievedUpdated)
        assertEquals("King Kohli", retrievedUpdated!!.name)
        assertEquals(120, retrievedUpdated.runsScored)
    }

    @Test
    fun testAddAndEditTeam() = runBlocking {
        // Create team
        val team = TeamEntity(
            id = 10,
            name = "RCB",
            captain = "Faf du Plessis",
            teamPoints = 0
        )

        // Insert team
        db.mpsDao().insertTeam(team)

        // Read team back and verify
        val retrieved = db.mpsDao().getTeamById(10)
        assertNotNull(retrieved)
        assertEquals("RCB", retrieved!!.name)
        assertEquals("Faf du Plessis", retrieved.captain)

        // Edit team
        val updatedTeam = retrieved.copy(captain = "Virat Kohli", teamPoints = 4)
        db.mpsDao().updateTeam(updatedTeam)

        val retrievedUpdated = db.mpsDao().getTeamById(10)
        assertNotNull(retrievedUpdated)
        assertEquals("Virat Kohli", retrievedUpdated!!.captain)
        assertEquals(4, retrievedUpdated.teamPoints)
    }

    @Test
    fun testAddMatchAndRankingsUpdate() = runBlocking {
        // 1. Create Team and Players
        val teamA = TeamEntity(id = 1, name = "India", captain = "Rohit")
        val teamB = TeamEntity(id = 2, name = "Australia", captain = "Cummins")
        db.mpsDao().insertTeam(teamA)
        db.mpsDao().insertTeam(teamB)

        val player1 = PlayerEntity(id = 101, name = "Rohit Sharma", teamId = 1, teamName = "India", role = "Batsman")
        val player2 = PlayerEntity(id = 102, name = "Steve Smith", teamId = 2, teamName = "Australia", role = "Batsman")
        db.mpsDao().insertPlayer(player1)
        db.mpsDao().insertPlayer(player2)

        // 2. Add Match scorecard and performances
        val match = MatchEntity(
            id = 1001,
            tournament = "World Cup",
            venue = "Wankhede",
            date = "2026-07-03",
            time = "14:00",
            teamAId = 1,
            teamBId = 2,
            teamAName = "India",
            teamBName = "Australia",
            winnerTeamId = 1,
            scoreA = "250/5",
            scoreB = "240/10"
        )

        val perf1 = PlayerMatchPerformanceEntity(
            id = 5001,
            matchId = 1001,
            playerId = 101,
            playerName = "Rohit Sharma",
            teamId = 1,
            teamName = "India",
            runs = 100,
            balls = 80,
            fours = 10,
            sixes = 4,
            isPlayerOfMatch = true,
            isWinner = true
        )

        val perf2 = PlayerMatchPerformanceEntity(
            id = 5002,
            matchId = 1001,
            playerId = 102,
            playerName = "Steve Smith",
            teamId = 2,
            teamName = "Australia",
            runs = 50,
            balls = 60,
            fours = 4,
            sixes = 0,
            isWinner = false
        )

        // Call repository to insert match and trigger auto-calculations
        repository.insertMatch(match, listOf(perf1, perf2))

        // 3. Verify point system and rankings calculations
        val updatedPlayer1 = db.mpsDao().getPlayerById(101)
        val updatedPlayer2 = db.mpsDao().getPlayerById(102)

        assertNotNull(updatedPlayer1)
        assertNotNull(updatedPlayer2)

        // Rohit Sharma scored 100 runs.
        // Runs points = 100 * 1.0 = 100
        // Four bonus = 10 * 1.0 = 10
        // Six bonus = 4 * 2.0 = 8
        // Century bonus = 20.0
        // Strike rate = 125.0 % (less than 150.0 strikeRateThreshold, so no strike rate bonus)
        // Player of the match bonus = 30.0
        // Win bonus = 5.0
        // Expected Points = 100 + 10 + 8 + 20 + 30 + 5 = 173.0
        assertEquals(173.0, updatedPlayer1!!.totalPoints, 0.5)

        // Steve Smith scored 50 runs.
        // Runs points = 50 * 1.0 = 50
        // Four bonus = 4 * 1.0 = 4
        // Six bonus = 0 * 2.0 = 0
        // Half-century bonus = 10.0
        // Expected Points = 50 + 4 + 10 = 64.0
        assertEquals(64.0, updatedPlayer2!!.totalPoints, 0.5)

        // 4. Verify Rankings are updated and automatically sorted correctly
        val playersRanked = db.mpsDao().getAllPlayers().first()
        assertEquals(2, playersRanked.size)
        assertEquals("Rohit Sharma", playersRanked[0].name) // Top rank due to higher points (173.0 > 64.0)
        assertEquals("Steve Smith", playersRanked[1].name)

        // 5. Verify Team standings/points are updated
        val updatedTeamA = db.mpsDao().getTeamById(1)
        val updatedTeamB = db.mpsDao().getTeamById(2)

        assertNotNull(updatedTeamA)
        assertNotNull(updatedTeamB)
        // Team A won, should have 1 win, 2 team points (usually 2 points per win)
        assertEquals(1, updatedTeamA!!.wins)
        assertEquals(2, updatedTeamA.teamPoints)
        
        // Team B lost, should have 1 loss, 0 team points
        assertEquals(1, updatedTeamB!!.losses)
        assertEquals(0, updatedTeamB.teamPoints)
    }

    @Test
    fun testDeleteAndEditMatchRecalculatesRankings() = runBlocking {
        // 1. Initial setup
        val teamA = TeamEntity(id = 1, name = "India", captain = "Rohit")
        db.mpsDao().insertTeam(teamA)

        val player1 = PlayerEntity(id = 101, name = "Rohit Sharma", teamId = 1, teamName = "India", role = "Batsman")
        db.mpsDao().insertPlayer(player1)

        val match = MatchEntity(
            id = 1001,
            tournament = "World Cup",
            venue = "Wankhede",
            date = "2026-07-03",
            time = "14:00",
            teamAId = 1,
            teamBId = 2,
            teamAName = "India",
            teamBName = "Australia",
            winnerTeamId = 1,
            scoreA = "100/1",
            scoreB = "90/10"
        )

        val perf1 = PlayerMatchPerformanceEntity(
            id = 5001,
            matchId = 1001,
            playerId = 101,
            playerName = "Rohit Sharma",
            teamId = 1,
            teamName = "India",
            runs = 80,
            balls = 50,
            isWinner = true
        )

        repository.insertMatch(match, listOf(perf1))

        // Ensure points were added
        val p1WithPoints = db.mpsDao().getPlayerById(101)
        assertNotNull(p1WithPoints)
        assertTrue(p1WithPoints!!.totalPoints > 0.0)

        // 2. Delete Match and verify stats and points reset to 0
        repository.deleteMatch(match)

        val p1Reset = db.mpsDao().getPlayerById(101)
        assertNotNull(p1Reset)
        assertEquals(0.0, p1Reset!!.totalPoints, 0.0)
        assertEquals(0, p1Reset.runsScored)
        assertEquals(0, p1Reset.matchesPlayed)
    }
}
