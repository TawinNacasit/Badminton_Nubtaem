package com.nubtam.badmintonscore.data

import android.content.Context
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.P2pClient
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import com.nubtam.badmintonscore.BuildConfig
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Wear Engine adapter based on Huawei's P2pClient sample/API.
 * Service approval, signing fingerprint and an installed peer app are prerequisites.
 * The application starts with MockTransport; opt in only after configuring the real device.
 */
class WearEngineTransport(context: Context) : ScoreTransport {
    override val label = "Huawei Wear Engine"
    private val appContext = context.applicationContext
    private val p2pClient: P2pClient = HiWear.getP2pClient(appContext)
    private var device: Device? = null
    private var receiver: Receiver? = null

    override fun connect(onStatus: (ConnectionStatus, String?) -> Unit, onState: (ScoreState) -> Unit) {
        if (BuildConfig.WEAR_PEER_PACKAGE.isBlank() || BuildConfig.WEAR_PEER_FINGERPRINT.isBlank()) {
            onStatus(ConnectionStatus.ERROR, "ยังไม่ได้ตั้งค่า peer package/fingerprint")
            return
        }
        onStatus(ConnectionStatus.SYNCING, "กำลังค้นหาอุปกรณ์ที่จับคู่")
        p2pClient.setPeerPkgName(BuildConfig.WEAR_PEER_PACKAGE)
        p2pClient.setPeerFingerPrint(BuildConfig.WEAR_PEER_FINGERPRINT)
        HiWear.getDeviceClient(appContext).bondedDevices
            .addOnSuccessListener { devices ->
                val connected = devices.firstOrNull { it?.isConnected == true }
                if (connected == null) {
                    onStatus(ConnectionStatus.DISCONNECTED, "ไม่พบนาฬิกา Wear Engine ที่เชื่อมต่อ")
                    return@addOnSuccessListener
                }
                device = connected
                receiver = Receiver { message ->
                    if (message.type == Message.MESSAGE_TYPE_DATA) {
                        parseState(String(message.data, StandardCharsets.UTF_8))?.let(onState)
                    }
                }
                p2pClient.registerReceiver(connected, receiver)
                    .addOnSuccessListener { onStatus(ConnectionStatus.CONNECTED, connected.name) }
                    .addOnFailureListener { onStatus(ConnectionStatus.ERROR, it.message) }
            }
            .addOnFailureListener { onStatus(ConnectionStatus.ERROR, it.message) }
    }

    override fun send(command: ScoreCommand, onResult: (Result<Unit>) -> Unit) {
        val target = device ?: return onResult(Result.failure(IllegalStateException("No connected wearable")))
        val message = Message.Builder().setPayload(command.toJson().toByteArray(StandardCharsets.UTF_8)).build()
        p2pClient.send(target, message, object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode == 207) onResult(Result.success(Unit))
                else onResult(Result.failure(IllegalStateException("Wear Engine result code $resultCode")))
            }
            override fun onSendProgress(progress: Long) = Unit
        }).addOnFailureListener { onResult(Result.failure(it)) }
    }

    override fun close() {
        receiver?.let { p2pClient.unregisterReceiver(it) }
        receiver = null
        device = null
    }

    private fun ScoreCommand.toJson() = JSONObject().apply {
        put("protocolVersion", 1); put("type", "SCORE_COMMAND"); put("matchId", matchId)
        put("commandId", commandId); put("senderId", senderId); put("baseRevision", baseRevision)
        put("action", action); team?.let { put("team", it) }; put("timestamp", timestamp)
    }.toString()

    private fun parseState(raw: String): ScoreState? = runCatching {
        val json = JSONObject(raw)
        require(json.optInt("protocolVersion") == 1 && json.optString("type") == "SCORE_STATE")
        ScoreState(
            matchId = json.getString("matchId"), revision = json.getInt("revision"),
            teamA = json.getInt("teamA"), teamB = json.getInt("teamB"),
            teamNameA = json.optString("teamNameA", "Team A"), teamNameB = json.optString("teamNameB", "Team B"),
            gameNumber = json.optInt("gameNumber", 1), gamesWonA = json.optInt("gamesWonA"),
            gamesWonB = json.optInt("gamesWonB"), gameOver = json.optBoolean("gameOver"),
            matchOver = json.optBoolean("matchOver"), setsToWin = json.optInt("setsToWin", 2),
            maxSets = json.optInt("maxSets", 3),
            mode = json.optString("mode", "BADMINTON_21"),
            lastCommandId = json.optString("lastCommandId").ifBlank { null },
        )
    }.getOrNull()
}
