package com.nubtam.badmintonscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { MaterialTheme { Scoreboard() } }
    }
}

@Composable
private fun Scoreboard(vm: ScoreViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    var settingsOpen by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmNextSet by remember { mutableStateOf(false) }
    var confirmNewMatch by remember { mutableStateOf(false) }

    val incrementA = {
        when {
            ui.score.matchOver -> confirmNewMatch = true
            ui.score.gameOver -> confirmNextSet = true
            else -> vm.score("A", 1)
        }
    }
    val incrementB = {
        when {
            ui.score.matchOver -> confirmNewMatch = true
            ui.score.gameOver -> confirmNextSet = true
            else -> vm.score("B", 1)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth

        if (portrait) {
            Column(Modifier.fillMaxSize()) {
                ScoreSide(
                    score = ui.score.teamA,
                    color = Color(ui.colorA),
                    sideDescription = "Red side",
                    onIncrement = incrementA,
                    onDecrement = { vm.score("A", -1) },
                    modifier = Modifier.weight(1f),
                )
                ScoreSide(
                    score = ui.score.teamB,
                    color = Color(ui.colorB),
                    sideDescription = "Blue side",
                    onIncrement = incrementB,
                    onDecrement = { vm.score("B", -1) },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                ScoreSide(
                    score = ui.score.teamA,
                    color = Color(ui.colorA),
                    sideDescription = "Red side",
                    onIncrement = incrementA,
                    onDecrement = { vm.score("A", -1) },
                    modifier = Modifier.weight(1f),
                )
                ScoreSide(
                    score = ui.score.teamB,
                    color = Color(ui.colorB),
                    sideDescription = "Blue side",
                    onIncrement = incrementB,
                    onDecrement = { vm.score("B", -1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        IconButton(
            onClick = { settingsOpen = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(38.dp)
                .background(Color.Black.copy(alpha = 0.28f), CircleShape),
        ) {
            Text("⚙", color = Color.White, fontSize = 20.sp)
        }

        Surface(
            color = Color.Black.copy(alpha = 0.30f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
        ) {
            Text(
                text = "เซต ${ui.score.gameNumber}  •  ${ui.score.gamesWonA}–${ui.score.gamesWonB}" +
                    when {
                        ui.score.matchOver -> "  •  จบเกม"
                        ui.score.gameOver -> "  •  จบเซต"
                        else -> ""
                    },
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }

    if (settingsOpen) SettingsPanel(
        ui = ui,
        vm = vm,
        onResetRequest = { settingsOpen = false; confirmReset = true },
        onDismiss = { settingsOpen = false },
    )

    if (confirmNextSet) AlertDialog(
        onDismissRequest = { confirmNextSet = false },
        title = { Text("เริ่มเซตถัดไป?") },
        text = { Text("เซตก่อนหน้าถูกบันทึกในประวัติแล้ว คะแนนจะเริ่มใหม่ที่ 0–0") },
        confirmButton = {
            TextButton(onClick = { vm.startNextSet(); confirmNextSet = false }) { Text("เริ่มเซตถัดไป") }
        },
        dismissButton = {
            TextButton(onClick = { confirmNextSet = false }) { Text("ยังไม่เริ่ม") }
        },
    )

    if (confirmNewMatch) AlertDialog(
        onDismissRequest = { confirmNewMatch = false },
        title = { Text("เกมนี้จบแล้ว") },
        text = {
            Text(
                if (ui.score.gamesWonA == ui.score.gamesWonB) {
                    "เล่นครบ ${ui.score.maxSets} เซตแล้ว ผลเสมอ ${ui.score.gamesWonA}–${ui.score.gamesWonB} ต้องการเริ่มเกมใหม่หรือไม่?"
                } else {
                    "มีผู้ชนะเกมแล้ว ต้องการเริ่มเกมใหม่หรือไม่?"
                },
            )
        },
        confirmButton = {
            TextButton(onClick = { vm.reset(); confirmNewMatch = false }) { Text("เริ่มเกมใหม่") }
        },
        dismissButton = {
            TextButton(onClick = { confirmNewMatch = false }) { Text("ปิด") }
        },
    )

    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("เริ่มแมตช์ใหม่?") },
        text = { Text("คะแนนและจำนวนเซตที่ชนะทั้งหมดจะกลับเป็นศูนย์") },
        confirmButton = {
            TextButton(onClick = { vm.reset(); confirmReset = false }) {
                Text("เริ่มใหม่", color = Color(ui.colorA))
            }
        },
        dismissButton = {
            TextButton(onClick = { confirmReset = false }) { Text("ยกเลิก") }
        },
    )
}

@Composable
private fun ScoreSide(
    score: Int,
    color: Color,
    sideDescription: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 64.dp.toPx() }
    val touchSlop = with(density) { 12.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(color)
            .semantics {
                contentDescription = "$sideDescription, score $score. Tap to add a point. Swipe left to remove a point."
                onClick {
                    onIncrement()
                    true
                }
            }
            .pointerInput(onIncrement, onDecrement, swipeThreshold, touchSlop) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var horizontalDistance = 0f
                    var verticalDistance = 0f

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange()
                        horizontalDistance += delta.x
                        verticalDistance += delta.y
                    } while (change.pressed)

                    val horizontalGesture = abs(horizontalDistance) > abs(verticalDistance)
                    when {
                        horizontalGesture && horizontalDistance <= -swipeThreshold -> onDecrement()
                        abs(horizontalDistance) <= touchSlop && abs(verticalDistance) <= touchSlop -> onIncrement()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val scoreSize = min(maxWidth.value * 0.58f, maxHeight.value * 0.38f)
            .coerceIn(72f, 190f)

        Text(
            text = score.toString(),
            color = Color.White,
            fontSize = scoreSize.sp,
            fontWeight = FontWeight.Black,
            lineHeight = scoreSize.sp,
        )
    }
}
