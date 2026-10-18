package ru.redbyte.life3d.ui.start

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.redbyte.life3d.R
import ru.redbyte.life3d.domain.CellColors
import ru.redbyte.life3d.domain.Hsv
import ru.redbyte.life3d.ui.theme.LifeColors
import ru.redbyte.life3d.ui.theme.color

@Composable
fun GradientDialog(initial: CellColors, onDone: (CellColors) -> Unit, onDismiss: () -> Unit) {
    var start by remember { mutableStateOf(Hsv.of(initial.start)) }
    var end by remember { mutableStateOf(Hsv.of(initial.end)) }
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .clip(shape)
                .background(LifeColors.Background)
                .border(1.dp, LifeColors.PanelBorder, shape)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.gradient_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeColors.Text,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(start.toRgb().color, end.toRgb().color))),
            )
            ColorEditor(stringResource(R.string.gradient_start), start) { start = it }
            ColorEditor(stringResource(R.string.gradient_end), end) { end = it }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = LifeColors.TextDim)
                }
                Spacer(Modifier.size(8.dp))
                TextButton(onClick = { onDone(CellColors(start.toRgb(), end.toRgb())) }) {
                    Text(stringResource(R.string.done), color = start.toRgb().color, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ColorEditor(title: String, hsv: Hsv, onChange: (Hsv) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(hsv.toRgb().color)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            )
            Spacer(Modifier.size(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = LifeColors.Text)
        }
        TrackSlider(
            label = stringResource(R.string.gradient_hue),
            value = hsv.hue,
            range = 0f..HUE_MAX,
            track = Brush.horizontalGradient(RAINBOW.map { Hsv(it, 1f, 1f).toRgb().color }),
            thumb = Hsv(hsv.hue, 1f, 1f).toRgb().color,
        ) { onChange(hsv.copy(hue = it)) }
        TrackSlider(
            label = stringResource(R.string.gradient_saturation),
            value = hsv.saturation,
            range = 0f..1f,
            track = Brush.horizontalGradient(
                listOf(hsv.copy(saturation = 0f).toRgb().color, hsv.copy(saturation = 1f).toRgb().color),
            ),
            thumb = hsv.toRgb().color,
        ) { onChange(hsv.copy(saturation = it)) }
        TrackSlider(
            label = stringResource(R.string.gradient_brightness),
            value = hsv.value,
            range = MIN_BRIGHTNESS..1f,
            track = Brush.horizontalGradient(
                listOf(hsv.copy(value = MIN_BRIGHTNESS).toRgb().color, hsv.copy(value = 1f).toRgb().color),
            ),
            thumb = hsv.toRgb().color,
        ) { onChange(hsv.copy(value = it)) }
    }
}

@Composable
private fun TrackSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    track: Brush,
    thumb: Color,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LifeColors.TextDim)
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(track),
            )
            Slider(
                value = value.coerceIn(range),
                onValueChange = onChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = thumb,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}

private const val HUE_MAX = 359.9f
private const val MIN_BRIGHTNESS = 0.25f
private val RAINBOW = listOf(0f, 60f, 120f, 180f, 240f, 300f, 359.9f)
