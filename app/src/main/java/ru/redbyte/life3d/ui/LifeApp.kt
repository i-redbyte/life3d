package ru.redbyte.life3d.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import ru.redbyte.life3d.domain.CellPalette
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.LifeRule
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.StartCells
import ru.redbyte.life3d.ui.game.GameScreen
import ru.redbyte.life3d.ui.start.StartScreen
import ru.redbyte.life3d.ui.theme.LifeTheme
import ru.redbyte.life3d.ui.theme.color

/** Two screens: the settings and the field. Both survive rotation and process death. */
@Composable
fun LifeApp() {
    var settings by rememberSaveable(stateSaver = SettingsSaver) { mutableStateOf(LifeSettings()) }
    var playing by rememberSaveable { mutableStateOf(false) }
    LifeTheme(accent = settings.palette.color) {
        AnimatedContent(
            targetState = playing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { inGame ->
            if (inGame) {
                GameScreen(settings = settings, onBack = { playing = false })
            } else {
                StartScreen(settings = settings, onChange = { settings = it }, onStart = { playing = true })
            }
        }
    }
}

private val SettingsSaver: Saver<LifeSettings, Any> = listSaver(
    save = { listOf(it.palette.name, it.rule.name, it.gridSize, it.start.count, it.start.random, it.speed, it.flow, it.mode.name) },
    restore = { saved ->
        LifeSettings(
            palette = CellPalette.valueOf(saved[0] as String),
            rule = LifeRule.valueOf(saved[1] as String),
            gridSize = saved[2] as Int,
            start = StartCells(count = saved[3] as Int, random = saved[4] as Boolean),
            speed = saved[5] as Int,
            flow = saved[6] as Boolean,
            mode = FieldMode.valueOf(saved[7] as String),
        )
    },
)
