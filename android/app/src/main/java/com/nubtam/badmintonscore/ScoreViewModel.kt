package com.nubtam.badmintonscore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nubtam.badmintonscore.data.ConnectionStatus
import com.nubtam.badmintonscore.data.MockTransport
import com.nubtam.badmintonscore.data.ScoreCommand
import com.nubtam.badmintonscore.data.ScoreState
import com.nubtam.badmintonscore.data.ScoreTransport
import com.nubtam.badmintonscore.data.SetHistoryEntry
import com.nubtam.badmintonscore.data.WearEngineTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val DefaultColorA = 0xFFD71920.toInt()
private val DefaultColorB = 0xFF0067B9.toInt()

data class ScoreUiState(
    val score: ScoreState = ScoreState(),
    val connection: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val connectionDetail: String = "",
    val transportLabel: String = "Mock transport",
    val lastSync: Long? = null,
    val setHistory: List<SetHistoryEntry> = emptyList(),
    val colorA: Int = DefaultColorA,
    val colorB: Int = DefaultColorB,
)

class ScoreViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("last_match", 0)
    private val historyPrefs = application.getSharedPreferences("set_history", 0)
    private val appearancePrefs = application.getSharedPreferences("appearance", 0)
    private val _ui = MutableStateFlow(
        ScoreUiState(
            score = load(),
            setHistory = loadHistory(),
            colorA = appearancePrefs.getInt("colorA", DefaultColorA),
            colorB = appearancePrefs.getInt("colorB", DefaultColorB),
        ),
    )
    val ui = _ui.asStateFlow()
    private var transport: ScoreTransport = MockTransport()
    private val history = ArrayDeque<ScoreState>()

    init { connect(transport) }

    fun useMock() = connect(MockTransport())
    fun useWearEngine() = connect(WearEngineTransport(getApplication()))

    private fun connect(next: ScoreTransport) {
        transport.close()
        transport = next
        _ui.value = _ui.value.copy(transportLabel = next.label, connection = ConnectionStatus.SYNCING)
        next.connect(
            onStatus = { status, detail -> _ui.value = _ui.value.copy(connection = status, connectionDetail = detail.orEmpty()) },
            onState = { state -> update(state) },
        )
    }

    fun score(team: String, delta: Int) {
        val state = _ui.value.score
        if (state.gameOver) return
        val action = if (delta > 0) "INCREMENT" else "DECREMENT"
        val command = ScoreCommand(state.matchId, UUID.randomUUID().toString(), "android-01", state.revision, action, team)
        if (transport is MockTransport) {
            val current = if (team == "A") state.teamA else state.teamB
            val nextValue = (current + delta).coerceAtLeast(0)
            if (nextValue == current) return
            history.addLast(state)
            while (history.size > 100) history.removeFirst()
            var next = if (team == "A") {
                state.copy(teamA = nextValue, revision = state.revision + 1, lastCommandId = command.commandId)
            } else {
                state.copy(teamB = nextValue, revision = state.revision + 1, lastCommandId = command.commandId)
            }
            if (delta > 0 && isWinningScore(
                    own = if (team == "A") next.teamA else next.teamB,
                    other = if (team == "A") next.teamB else next.teamA,
                )
            ) {
                next = if (team == "A") {
                    val wins = next.gamesWonA + 1
                    next.copy(
                        gamesWonA = wins,
                        gameOver = true,
                        matchOver = wins >= next.setsToWin || wins + next.gamesWonB >= next.maxSets,
                    )
                } else {
                    val wins = next.gamesWonB + 1
                    next.copy(
                        gamesWonB = wins,
                        gameOver = true,
                        matchOver = wins >= next.setsToWin || next.gamesWonA + wins >= next.maxSets,
                    )
                }
                addHistory(next, team)
            }
            update(next)
        }
        transport.send(command) { result ->
            if (result.isFailure) _ui.value = _ui.value.copy(connection = ConnectionStatus.ERROR, connectionDetail = result.exceptionOrNull()?.message.orEmpty())
        }
    }

    fun undo() {
        if (transport is MockTransport) history.removeLastOrNull()?.let(::update)
        else send("UNDO")
    }

    fun reset() {
        if (transport is MockTransport) {
            history.clear()
            update(
                ScoreState(
                    matchId = "match-${System.currentTimeMillis()}",
                    teamNameA = _ui.value.score.teamNameA,
                    teamNameB = _ui.value.score.teamNameB,
                    setsToWin = _ui.value.score.setsToWin,
                    maxSets = _ui.value.score.maxSets,
                ),
            )
        } else send("RESET_MATCH")
    }

    fun startNextSet() {
        val state = _ui.value.score
        if (!state.gameOver || state.matchOver) return
        if (transport is MockTransport) {
            history.addLast(state)
            update(
                state.copy(
                    teamA = 0,
                    teamB = 0,
                    gameNumber = state.gameNumber + 1,
                    gameOver = false,
                    matchOver = false,
                    revision = state.revision + 1,
                ),
            )
        } else {
            send("START_NEXT_GAME")
        }
    }

    fun setSetsToWin(value: Int) {
        val target = value.coerceIn(2, 3)
        val state = _ui.value.score
        update(
            state.copy(
                setsToWin = target,
                matchOver = maxOf(state.gamesWonA, state.gamesWonB) >= target,
                revision = state.revision + 1,
            ),
        )
    }

    fun setMatchFormat(maxSets: Int) {
        val totalSets = when (maxSets) { 2 -> 2; 5 -> 5; else -> 3 }
        val targetWins = if (totalSets == 5) 3 else 2
        val state = _ui.value.score
        update(
            state.copy(
                setsToWin = targetWins,
                maxSets = totalSets,
                matchOver = maxOf(state.gamesWonA, state.gamesWonB) >= targetWins ||
                    state.gamesWonA + state.gamesWonB >= totalSets,
                revision = state.revision + 1,
            ),
        )
    }

    fun setColors(colorA: Int, colorB: Int) {
        appearancePrefs.edit().putInt("colorA", colorA).putInt("colorB", colorB).apply()
        _ui.value = _ui.value.copy(colorA = colorA, colorB = colorB)
    }

    fun setSideColor(team: String, color: Int) {
        if (team == "A") setColors(color, _ui.value.colorB) else setColors(_ui.value.colorA, color)
    }

    fun deleteHistory(id: String) {
        val next = _ui.value.setHistory.filterNot { it.id == id }
        _ui.value = _ui.value.copy(setHistory = next)
        saveHistory(next)
    }

    fun clearHistory() {
        _ui.value = _ui.value.copy(setHistory = emptyList())
        saveHistory(emptyList())
    }

    private fun send(action: String) {
        val state = _ui.value.score
        transport.send(ScoreCommand(state.matchId, UUID.randomUUID().toString(), "android-01", state.revision, action)) { result ->
            if (result.isFailure) _ui.value = _ui.value.copy(connection = ConnectionStatus.ERROR, connectionDetail = result.exceptionOrNull()?.message.orEmpty())
        }
    }

    private fun update(state: ScoreState) {
        _ui.value = _ui.value.copy(score = state, lastSync = System.currentTimeMillis())
        prefs.edit().putString("state", JSONObject().apply {
            put("matchId", state.matchId); put("revision", state.revision); put("teamA", state.teamA); put("teamB", state.teamB)
            put("teamNameA", state.teamNameA); put("teamNameB", state.teamNameB); put("gameNumber", state.gameNumber)
            put("gamesWonA", state.gamesWonA); put("gamesWonB", state.gamesWonB); put("gameOver", state.gameOver)
            put("matchOver", state.matchOver); put("setsToWin", state.setsToWin); put("maxSets", state.maxSets); put("mode", state.mode)
        }.toString()).apply()
    }

    private fun load(): ScoreState = runCatching {
        val json = JSONObject(prefs.getString("state", null) ?: return ScoreState())
        ScoreState(
            matchId = json.getString("matchId"), revision = json.getInt("revision"), teamA = json.getInt("teamA"), teamB = json.getInt("teamB"),
            teamNameA = json.getString("teamNameA"), teamNameB = json.getString("teamNameB"), gameNumber = json.getInt("gameNumber"),
            gamesWonA = json.getInt("gamesWonA"), gamesWonB = json.getInt("gamesWonB"), gameOver = json.optBoolean("gameOver"),
            matchOver = json.optBoolean("matchOver"), setsToWin = json.optInt("setsToWin", 2),
            maxSets = json.optInt("maxSets", 3), mode = json.getString("mode"),
        )
    }.getOrDefault(ScoreState())

    private fun addHistory(state: ScoreState, winner: String) {
        val entry = SetHistoryEntry(
            id = UUID.randomUUID().toString(),
            matchId = state.matchId,
            setNumber = state.gameNumber,
            scoreA = state.teamA,
            scoreB = state.teamB,
            winner = winner,
            completedAt = System.currentTimeMillis(),
        )
        val next = listOf(entry) + _ui.value.setHistory
        _ui.value = _ui.value.copy(setHistory = next)
        saveHistory(next)
    }

    private fun saveHistory(entries: List<SetHistoryEntry>) {
        val json = JSONArray()
        entries.forEach { entry ->
            json.put(JSONObject().apply {
                put("id", entry.id); put("matchId", entry.matchId); put("setNumber", entry.setNumber)
                put("scoreA", entry.scoreA); put("scoreB", entry.scoreB); put("winner", entry.winner)
                put("completedAt", entry.completedAt)
            })
        }
        historyPrefs.edit().putString("entries", json.toString()).apply()
    }

    private fun loadHistory(): List<SetHistoryEntry> = runCatching {
        val json = JSONArray(historyPrefs.getString("entries", "[]"))
        List(json.length()) { index ->
            val entry = json.getJSONObject(index)
            SetHistoryEntry(
                id = entry.getString("id"), matchId = entry.getString("matchId"),
                setNumber = entry.getInt("setNumber"), scoreA = entry.getInt("scoreA"),
                scoreB = entry.getInt("scoreB"), winner = entry.getString("winner"),
                completedAt = entry.getLong("completedAt"),
            )
        }
    }.getOrDefault(emptyList())

    override fun onCleared() { transport.close() }

    private fun isWinningScore(own: Int, other: Int): Boolean =
        own == 30 || (own >= 21 && own - other >= 2)
}

