package com.nubtam.badmintonscore.data

interface ScoreTransport {
    val label: String
    fun connect(onStatus: (ConnectionStatus, String?) -> Unit, onState: (ScoreState) -> Unit)
    fun send(command: ScoreCommand, onResult: (Result<Unit>) -> Unit)
    fun close()
}

class MockTransport : ScoreTransport {
    override val label = "Mock transport"
    private var status: ((ConnectionStatus, String?) -> Unit)? = null

    override fun connect(onStatus: (ConnectionStatus, String?) -> Unit, onState: (ScoreState) -> Unit) {
        status = onStatus
        onStatus(ConnectionStatus.CONNECTED, "พร้อมใช้งานโดยไม่ต้องมีนาฬิกา")
    }

    override fun send(command: ScoreCommand, onResult: (Result<Unit>) -> Unit) = onResult(Result.success(Unit))
    override fun close() { status = null }
}

