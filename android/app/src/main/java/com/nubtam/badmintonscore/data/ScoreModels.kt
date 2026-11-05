package com.nubtam.badmintonscore.data

data class ScoreState(
    val matchId: String = "local-match-001",
    val revision: Int = 0,
    val teamA: Int = 0,
    val teamB: Int = 0,
    val teamNameA: String = "Team A",
    val teamNameB: String = "Team B",
    val gameNumber: Int = 1,
    val gamesWonA: Int = 0,
    val gamesWonB: Int = 0,
    val gameOver: Boolean = false,
    val matchOver: Boolean = false,
    val setsToWin: Int = 2,
    val maxSets: Int = 3,
    val mode: String = "BADMINTON_21",
    val lastCommandId: String? = null,
)

data class SetHistoryEntry(
    val id: String,
    val matchId: String,
    val setNumber: Int,
    val scoreA: Int,
    val scoreB: Int,
    val winner: String,
    val completedAt: Long,
)

data class ScoreCommand(
    val matchId: String,
    val commandId: String,
    val senderId: String,
    val baseRevision: Int,
    val action: String,
    val team: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class ConnectionStatus { CONNECTED, DISCONNECTED, SYNCING, ERROR }

