package com.nubtam.badmintonscore

import android.graphics.Color.colorToHSV
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nubtam.badmintonscore.data.SetHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private enum class SettingsPage { HOME, MATCH, COLORS, HISTORY, CONNECTION, SCORE }
private data class ScorePalette(val name: String, val colorA: Int, val colorB: Int)

private val palettes = listOf(
    ScorePalette("แดง–น้ำเงิน", 0xFFD71920.toInt(), 0xFF0067B9.toInt()),
    ScorePalette("ส้ม–เขียว", 0xFFF05A28.toInt(), 0xFF008C72.toInt()),
    ScorePalette("ม่วง–มินต์", 0xFF7137A8.toInt(), 0xFF00A896.toInt()),
    ScorePalette("ดำ–ทอง", 0xFF222222.toInt(), 0xFFD89B16.toInt()),
)

@Composable
internal fun SettingsPanel(
    ui: ScoreUiState,
    vm: ScoreViewModel,
    onResetRequest: () -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(SettingsPage.HOME) }
    var colorTarget by remember { mutableStateOf<String?>(null) }

    if (colorTarget != null) {
        val team = colorTarget.orEmpty()
        ColorPickerPage(
            title = if (team == "A") "สีฝั่ง A" else "สีฝั่ง B",
            initialColor = if (team == "A") ui.colorA else ui.colorB,
            onSave = { vm.setSideColor(team, it); colorTarget = null },
            onBack = { colorTarget = null },
            onClose = onDismiss,
        )
        return
    }

    when (page) {
        SettingsPage.HOME -> SettingsShell("ตั้งค่า", onClose = onDismiss) {
            Text(
                "ปรับเกมและหน้าจอคะแนน",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Column(
                modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SettingsMenuRow("◎", Color(0xFF6C4DFF), "รูปแบบเกม", matchFormatLabel(ui), { page = SettingsPage.MATCH })
                SettingsMenuRow("◐", Color(0xFFE04B5A), "สีสนามคะแนน", "ปรับชุดสีและวงล้อสี", { page = SettingsPage.COLORS }, ui.colorA, ui.colorB)
                SettingsMenuRow("↺", Color(0xFF008C72), "ประวัติ", "${ui.setHistory.size} เซตที่บันทึกไว้", { page = SettingsPage.HISTORY })
                SettingsMenuRow("⌁", Color(0xFF1478C8), "การเชื่อมต่อ", ui.transportLabel, { page = SettingsPage.CONNECTION })
                SettingsMenuRow("±", Color(0xFFD17B00), "จัดการคะแนน", scoreStatusLabel(ui), { page = SettingsPage.SCORE })
            }
        }

        SettingsPage.MATCH -> SettingsShell("รูปแบบเกม", onBack = { page = SettingsPage.HOME }, onClose = onDismiss) {
            PageBody {
                PageIntro("กำหนดจำนวนเซตของหนึ่งเกม", "ทุกเซตใช้กติกา 21 แต้ม ดิวส์ห่าง 2 แต้ม และแต้มสูงสุด 30")
                MatchOption(ui.score.maxSets == 2, "เล่นครบ 2 เซต", "จบ 2–0 หรือเสมอ 1–1") { vm.setMatchFormat(2) }
                MatchOption(ui.score.maxSets == 3, "ชนะ 2 เซตก่อน", "แข่งขันสูงสุด 3 เซต") { vm.setMatchFormat(3) }
                MatchOption(ui.score.maxSets == 5, "ชนะ 3 เซตก่อน", "แข่งขันสูงสุด 5 เซต") { vm.setMatchFormat(5) }
            }
        }

        SettingsPage.COLORS -> SettingsShell("สีสนามคะแนน", onBack = { page = SettingsPage.HOME }, onClose = onDismiss) {
            PageBody {
                PageIntro("สีที่มองเห็นได้ทันที", "เลือกชุดสีสำเร็จรูป หรือปรับแต่ละฝั่งด้วยวงล้อสี")
                Row(Modifier.fillMaxWidth().height(74.dp)) {
                    Box(Modifier.weight(1f).height(74.dp).background(Color(ui.colorA), RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)))
                    Box(Modifier.weight(1f).height(74.dp).background(Color(ui.colorB), RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)))
                }
                Text("ชุดสีสำเร็จรูป", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palettes.forEach { palette ->
                        OutlinedButton(onClick = { vm.setColors(palette.colorA, palette.colorB) }) {
                            MiniSwatch(Color(palette.colorA)); MiniSwatch(Color(palette.colorB))
                            Text(palette.name, modifier = Modifier.padding(start = 7.dp), fontSize = 12.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ColorSideButton("ฝั่ง A", Color(ui.colorA), Modifier.weight(1f)) { colorTarget = "A" }
                    ColorSideButton("ฝั่ง B", Color(ui.colorB), Modifier.weight(1f)) { colorTarget = "B" }
                }
            }
        }

        SettingsPage.HISTORY -> SettingsShell("ประวัติเซต", onBack = { page = SettingsPage.HOME }, onClose = onDismiss) {
            HistoryPage(ui.setHistory, vm::deleteHistory, vm::clearHistory)
        }

        SettingsPage.CONNECTION -> SettingsShell("การเชื่อมต่อ", onBack = { page = SettingsPage.HOME }, onClose = onDismiss) {
            PageBody {
                PageIntro("เชื่อมต่อคะแนน", "สถานะปัจจุบัน: ${ui.transportLabel}")
                ConnectionOption("จำลองบนโทรศัพท์", "ทดสอบกติกาและหน้าจอโดยไม่ใช้นาฬิกา", ui.transportLabel.contains("Mock"), vm::useMock)
                ConnectionOption("Huawei Wear Engine", "รับและส่งคะแนนกับนาฬิกาหลังได้รับสิทธิ์", ui.transportLabel.contains("Huawei"), vm::useWearEngine)
                if (ui.connectionDetail.isNotBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text(ui.connectionDetail, modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                    }
                }
            }
        }

        SettingsPage.SCORE -> SettingsShell("จัดการคะแนน", onBack = { page = SettingsPage.HOME }, onClose = onDismiss) {
            PageBody {
                PageIntro("คะแนนปัจจุบัน ${ui.score.teamA}–${ui.score.teamB}", "เซต ${ui.score.gameNumber} • ชนะแล้ว ${ui.score.gamesWonA}–${ui.score.gamesWonB}")
                if (ui.score.gameOver && !ui.score.matchOver) {
                    Button(onClick = vm::startNextSet, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(ui.colorB))) {
                        Text("เริ่มเซตถัดไป")
                    }
                }
                OutlinedButton(onClick = vm::undo, modifier = Modifier.fillMaxWidth()) { Text("ย้อนแต้มล่าสุด") }
                Surface(color = Color(0xFFFFE9E7), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("เริ่มเกมใหม่", color = Color(0xFF8F1D20), fontWeight = FontWeight.Bold)
                        Text("ล้างคะแนนและจำนวนเซต แต่ไม่ลบประวัติ", fontSize = 12.sp, color = Color(0xFF8F1D20))
                        TextButton(onClick = onResetRequest, modifier = Modifier.align(Alignment.End)) { Text("เริ่มใหม่", color = Color(0xFFB3261E)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsShell(
    title: String,
    onBack: (() -> Unit)? = null,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) TextButton(onClick = onBack) { Text("‹  กลับ") }
                    else Spacer(Modifier.size(64.dp))
                    Text(title, modifier = Modifier.weight(1f), fontSize = 21.sp, fontWeight = FontWeight.Black)
                    TextButton(onClick = onClose) { Text("ปิด") }
                }
                content()
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PageBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun PageIntro(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsMenuRow(
    symbol: String,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    swatchA: Int? = null,
    swatchB: Int? = null,
) {
    Surface(
        color = accent.copy(alpha = 0.09f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(accent, CircleShape), contentAlignment = Alignment.Center) {
                Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            if (swatchA != null && swatchB != null) {
                MiniSwatch(Color(swatchA)); MiniSwatch(Color(swatchB))
            } else Text("›", color = accent, fontSize = 28.sp)
        }
    }
}

@Composable
private fun MatchOption(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    val accent = Color(0xFF6C4DFF)
    Surface(
        color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().border(if (selected) 2.dp else 0.dp, accent, RoundedCornerShape(18.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).border(2.dp, accent, CircleShape).padding(4.dp).background(if (selected) accent else Color.Transparent, CircleShape),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ColorSideButton(label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(52.dp)) {
        MiniSwatch(color); Text("ปรับ $label", modifier = Modifier.padding(start = 7.dp))
    }
}

@Composable
private fun ConnectionOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFF1478C8).copy(alpha = 0.13f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (selected) Color(0xFF1478C8) else Color.Gray, CircleShape))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryPage(entries: List<SetHistoryEntry>, onDelete: (String) -> Unit, onClear: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (entries.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("↺", fontSize = 34.sp, color = Color(0xFF008C72))
                    Text("ยังไม่มีประวัติ", fontWeight = FontWeight.Bold)
                    Text("เมื่อจบเซต รายการจะปรากฏที่นี่", fontSize = 12.sp)
                }
            }
        } else {
            entries.forEach { entry ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 14.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("เซต ${entry.setNumber}  •  ${entry.scoreA}–${entry.scoreB}", fontWeight = FontWeight.Bold)
                            Text("ฝั่ง ${entry.winner} ชนะ  •  ${formatter.format(Date(entry.completedAt))}", fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDelete(entry.id) }) { Text("ลบ") }
                    }
                }
            }
            TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) { Text("ล้างประวัติทั้งหมด") }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("ล้างประวัติทั้งหมด?") },
        text = { Text("รายการประวัติเซตทั้งหมดจะถูกลบ") },
        confirmButton = { TextButton(onClick = { onClear(); confirmClear = false }) { Text("ล้างทั้งหมด") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("ยกเลิก") } },
    )
}

@Composable
private fun ColorPickerPage(title: String, initialColor: Int, onSave: (Int) -> Unit, onBack: () -> Unit, onClose: () -> Unit) {
    val initialHsv = remember(initialColor) { FloatArray(3).also { colorToHSV(initialColor, it) } }
    var hue by remember(initialColor) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialColor) { mutableFloatStateOf(initialHsv[2]) }
    val selected = Color.hsv(hue, saturation, brightness)

    SettingsShell(title, onBack = onBack, onClose = onClose) {
        Column(Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ColorWheel(hue, saturation) { h, s -> hue = h; saturation = s }
            Spacer(Modifier.height(10.dp))
            Text("ความสว่าง ${(brightness * 100).toInt()}%", modifier = Modifier.align(Alignment.Start))
            Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 0.15f..1f)
            Surface(color = selected, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {}
            Button(onClick = { onSave(selected.toArgb()) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("บันทึกสี") }
        }
    }
}

@Composable
private fun MiniSwatch(color: Color) {
    Box(Modifier.size(16.dp).background(color, CircleShape))
}

@Composable
private fun ColorWheel(hue: Float, saturation: Float, onChange: (Float, Float) -> Unit) {
    var wheelSize by remember { mutableStateOf(IntSize.Zero) }

    fun update(position: Offset) {
        if (wheelSize.width == 0 || wheelSize.height == 0) return
        val center = Offset(wheelSize.width / 2f, wheelSize.height / 2f)
        val dx = position.x - center.x
        val dy = position.y - center.y
        val radius = min(wheelSize.width, wheelSize.height) / 2f
        val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
        onChange(((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f, (distance / radius).coerceIn(0f, 1f))
    }

    Canvas(
        Modifier.size(210.dp).onSizeChanged { wheelSize = it }.pointerInput(wheelSize) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                update(down.position)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    update(change.position)
                } while (change.pressed)
            }
        },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red), center), radius, center)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), center, radius), radius, center)
        val radians = hue / 180f * PI.toFloat()
        val marker = Offset(center.x + cos(radians) * radius * saturation, center.y + sin(radians) * radius * saturation)
        drawCircle(Color.White, 8.dp.toPx(), marker)
        drawCircle(Color.Black.copy(alpha = 0.7f), 8.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
    }
}

private fun matchFormatLabel(ui: ScoreUiState): String = when (ui.score.maxSets) {
    2 -> "เล่นครบ 2 เซต"
    5 -> "ชนะ 3 เซตก่อน • สูงสุด 5 เซต"
    else -> "ชนะ 2 เซตก่อน • สูงสุด 3 เซต"
}

private fun scoreStatusLabel(ui: ScoreUiState): String = when {
    ui.score.matchOver -> "เกมจบแล้ว • ${ui.score.gamesWonA}–${ui.score.gamesWonB} เซต"
    ui.score.gameOver -> "เซต ${ui.score.gameNumber} จบแล้ว"
    else -> "${ui.score.teamA}–${ui.score.teamB} • เซต ${ui.score.gameNumber}"
}
