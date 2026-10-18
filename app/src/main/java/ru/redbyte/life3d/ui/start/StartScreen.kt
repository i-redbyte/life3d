package ru.redbyte.life3d.ui.start

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.redbyte.life3d.R
import ru.redbyte.life3d.domain.CellPalette
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.LifeRule
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.StartCells
import ru.redbyte.life3d.ui.theme.LifeColors
import ru.redbyte.life3d.ui.theme.color
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StartScreen(
    settings: LifeSettings,
    onChange: (LifeSettings) -> Unit,
    onStart: () -> Unit,
) {
    val accent by animateColorAsState(settings.palette.color, label = "accent")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeColors.Background),
    ) {
        LiquidBackdrop(accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Title(accent)
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(LifeColors.Panel)
                    .border(1.dp, LifeColors.PanelBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                ModeSection(settings, onChange)
                PaletteSection(settings, onChange)
                RuleSection(settings, onChange)
                SizeSection(settings, onChange)
                StartCellsSection(settings, onChange)
                SpeedSection(settings, onChange)
                if (settings.mode == FieldMode.Torus) FlowSection(settings, onChange)
            }
            Spacer(Modifier.height(28.dp))
            StartButton(accent, onStart)
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Slow glowing blobs in the accent colour: a hint of the liquid the cells live in. */
@Composable
private fun LiquidBackdrop(accent: Color) {
    val turn by rememberInfiniteTransition(label = "liquid").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(BACKDROP_PERIOD_MS, easing = LinearEasing)),
        label = "turn",
    )
    Canvas(Modifier.fillMaxSize()) {
        val blobs = listOf(
            Triple(0.25f, 0.18f, 0.55f),
            Triple(0.8f, 0.45f, 0.45f),
            Triple(0.35f, 0.85f, 0.6f),
        )
        blobs.forEachIndexed { index, (x, y, radius) ->
            val phase = turn + index * 2.1f
            val centre = Offset(
                size.width * (x + 0.08f * cos(phase)),
                size.height * (y + 0.05f * sin(phase * 1.3f)),
            )
            val r = size.minDimension * radius
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.22f - index * 0.04f), Color.Transparent),
                    center = centre,
                    radius = r,
                ),
                radius = r,
                center = centre,
            )
        }
    }
}

@Composable
private fun Title(accent: Color) {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val compact = width < COMPACT_WIDTH_DP.dp
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = stringResource(R.string.title_life),
            style = TextStyle(
                brush = Brush.linearGradient(listOf(Color.White, accent)),
                fontSize = if (compact) 52.sp else 64.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
                shadow = Shadow(color = accent.copy(alpha = 0.6f), blurRadius = 32f),
            ),
        )
        Text(
            text = stringResource(R.string.title_badge),
            modifier = Modifier
                .padding(start = 6.dp, top = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = LifeColors.Background,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        text = stringResource(R.string.subtitle),
        modifier = Modifier.widthIn(max = 420.dp),
        color = LifeColors.TextDim,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = LifeColors.Text,
    )
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = LifeColors.TextDim)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModeSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.section_mode))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (mode in FieldMode.entries) {
                LifeChip(
                    selected = mode == settings.mode,
                    label = stringResource(if (mode == FieldMode.Torus) R.string.mode_torus else R.string.mode_infinite),
                    onClick = { onChange(settings.copy(mode = mode)) },
                )
            }
        }
        Caption(
            stringResource(
                if (settings.mode == FieldMode.Torus) R.string.mode_torus_hint else R.string.mode_infinite_hint,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.section_color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (palette in CellPalette.entries) {
                val selected = palette == settings.palette
                val name = stringResource(palette.label)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else LifeColors.PanelBorder, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color.White, palette.color), radius = 60f))
                        .selectable(selected, role = Role.RadioButton) { onChange(settings.copy(palette = palette)) }
                        .semantics { contentDescription = name },
                )
            }
        }
        Caption(stringResource(settings.palette.label))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.section_rule))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rule in LifeRule.entries) {
                LifeChip(
                    selected = rule == settings.rule,
                    label = stringResource(rule.label),
                    onClick = { onChange(settings.copy(rule = rule)) },
                )
            }
        }
        Text(
            text = stringResource(settings.rule.hint),
            style = MaterialTheme.typography.bodyMedium,
            color = LifeColors.Text,
        )
        Caption(stringResource(R.string.rule_notation, settings.rule.notation))
    }
}

@Composable
private fun SizeSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    val n = settings.gridSize
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(stringResource(R.string.section_size))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (size in LifeSettings.GRID_SIZES) {
                LifeChip(
                    selected = size == n,
                    label = "$size³",
                    onClick = {
                        val start = settings.start.copy(count = settings.start.count.coerceIn(StartCells.range(size)))
                        onChange(settings.copy(gridSize = size, start = start))
                    },
                )
            }
        }
        val total = n * n * n
        Caption(
            if (settings.mode == FieldMode.Torus) {
                stringResource(R.string.size_value, n, pluralStringResource(R.plurals.cells, total, total))
            } else {
                stringResource(R.string.size_window, n)
            },
        )
    }
}

@Composable
private fun StartCellsSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    val range = StartCells.range(settings.gridSize)
    val start = settings.start
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionTitle(stringResource(R.string.section_start))
                Caption(
                    if (start.random) {
                        stringResource(
                            R.string.start_random_value,
                            range.first,
                            pluralStringResource(R.plurals.cells, range.last, range.last),
                        )
                    } else {
                        pluralStringResource(R.plurals.cells, start.count, start.count)
                    },
                )
            }
            Text(
                text = stringResource(R.string.start_random),
                style = MaterialTheme.typography.labelLarge,
                color = LifeColors.TextDim,
                modifier = Modifier.padding(end = 8.dp),
            )
            LifeSwitch(start.random) { onChange(settings.copy(start = start.copy(random = it))) }
        }
        LifeSlider(
            value = start.count.toFloat(),
            range = range.first.toFloat()..range.last.toFloat(),
            enabled = !start.random,
            onChange = { onChange(settings.copy(start = start.copy(count = it.roundToInt()))) },
        )
    }
}

@Composable
private fun SpeedSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    val range = LifeSettings.SPEED_RANGE
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionTitle(stringResource(R.string.section_speed))
        Caption(pluralStringResource(R.plurals.generations_per_second, settings.speed, settings.speed))
        LifeSlider(
            value = settings.speed.toFloat(),
            range = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            onChange = { onChange(settings.copy(speed = it.roundToInt().coerceIn(range))) },
        )
    }
}

@Composable
private fun FlowSection(settings: LifeSettings, onChange: (LifeSettings) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            SectionTitle(stringResource(R.string.section_flow))
            Caption(stringResource(R.string.flow_hint))
        }
        LifeSwitch(settings.flow) { onChange(settings.copy(flow = it)) }
    }
}

@Composable
private fun LifeChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = LifeColors.TextDim,
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            selectedLabelColor = MaterialTheme.colorScheme.primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = LifeColors.PanelBorder,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun LifeSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    Slider(
        value = value.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = LifeColors.PanelBorder,
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
}

@Composable
private fun LifeSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = LifeColors.Background,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedTrackColor = LifeColors.Background,
            uncheckedBorderColor = LifeColors.PanelBorder,
        ),
    )
}

/** A wide glowing pill that breathes slowly, in the chosen cell colour. */
@Composable
private fun StartButton(accent: Color, onStart: () -> Unit) {
    val breath by rememberInfiniteTransition(label = "start").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(BREATH_MS), RepeatMode.Reverse),
        label = "glow",
    )
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(68.dp)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = breath * 0.55f), Color.Transparent),
                        center = center,
                        radius = size.width * 0.6f,
                    ),
                    topLeft = Offset(-size.width * 0.1f, -size.height * 0.6f),
                    size = size.copy(width = size.width * 1.2f, height = size.height * 2.2f),
                )
            }
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(accent, Color.White.copy(alpha = 0.9f), accent)))
            .clickable(role = Role.Button, onClick = onStart),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "▶  " + stringResource(R.string.start_game),
            color = LifeColors.Background,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
        )
    }
}

private val CellPalette.label: Int
    get() = when (this) {
        CellPalette.Turquoise -> R.string.palette_turquoise
        CellPalette.Amber -> R.string.palette_amber
        CellPalette.Fuchsia -> R.string.palette_fuchsia
        CellPalette.Azure -> R.string.palette_azure
        CellPalette.Lime -> R.string.palette_lime
        CellPalette.Coral -> R.string.palette_coral
    }

private val LifeRule.label: Int
    get() = when (this) {
        LifeRule.Pulse -> R.string.rule_pulse
        LifeRule.Sponge -> R.string.rule_sponge
        LifeRule.Reef -> R.string.rule_reef
        LifeRule.Shells -> R.string.rule_shells
        LifeRule.Crystal -> R.string.rule_crystal
        LifeRule.Bays4555 -> R.string.rule_bays4555
    }

private val LifeRule.hint: Int
    get() = when (this) {
        LifeRule.Pulse -> R.string.rule_pulse_hint
        LifeRule.Sponge -> R.string.rule_sponge_hint
        LifeRule.Reef -> R.string.rule_reef_hint
        LifeRule.Shells -> R.string.rule_shells_hint
        LifeRule.Crystal -> R.string.rule_crystal_hint
        LifeRule.Bays4555 -> R.string.rule_bays4555_hint
    }

private const val BACKDROP_PERIOD_MS = 18_000
private const val BREATH_MS = 1_600
private const val COMPACT_WIDTH_DP = 360
