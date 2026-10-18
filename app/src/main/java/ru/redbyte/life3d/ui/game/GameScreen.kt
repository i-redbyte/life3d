package ru.redbyte.life3d.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import ru.redbyte.life3d.R
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.LifeSettings
import ru.redbyte.life3d.domain.OrbitCamera
import ru.redbyte.life3d.domain.torusCell
import ru.redbyte.life3d.domain.worldToCell
import ru.redbyte.life3d.engine.ControllerGpu
import ru.redbyte.life3d.engine.LifeEngine
import ru.redbyte.life3d.engine.LifeRenderer
import ru.redbyte.life3d.gl.lifeCompute
import ru.redbyte.life3d.gl.lifeScene
import ru.redbyte.life3d.ui.theme.LifeColors
import ru.redbyte.redbytefx.gl.compose.GlCompute
import ru.redbyte.redbytefx.gl.compose.GlLinkState
import ru.redbyte.redbytefx.gl.compose.GlSurface
import ru.redbyte.redbytefx.gl.compose.GlSurfaceConfig
import ru.redbyte.redbytefx.gl.compose.rememberGlController

@Composable
fun GameScreen(settings: LifeSettings, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val compute = remember { lifeCompute() }
    val scene = remember { lifeScene() }
    val computeController = rememberGlController(compute.program)
    val sceneController = rememberGlController(scene.program, GlSurfaceConfig(depth = true))
    val camera = remember { OrbitCamera() }
    val engine = remember(settings) {
        LifeEngine(settings, ControllerGpu(computeController, compute)).also { it.start() }
    }
    val renderer = remember(settings) { LifeRenderer(scene, engine, camera, settings) }

    val computeLink by computeController.linkState
    var paused by rememberSaveable { mutableStateOf(false) }
    var generation by remember { mutableIntStateOf(0) }
    var population by remember { mutableIntStateOf(engine.latest.population) }
    var hintVisible by remember { mutableStateOf(true) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(engine, paused, computeLink) {
        if (computeLink != GlLinkState.Linked || paused) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                engine.tick()
                generation = engine.generation
                population = engine.latest.population
                delay(settings.intervalMillis)
            }
        }
    }
    LaunchedEffect(Unit) {
        delay(HINT_MILLIS)
        hintVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        GlCompute(controller = computeController, modifier = Modifier.size(1.dp), overlay = {})
        GlSurface(
            controller = sceneController,
            mesh = renderer.backgroundMesh,
            modifier = Modifier
                .fillMaxSize()
                .fieldGestures(camera, settings.mode) { renderer.touchedAt = System.nanoTime() }
                .pointerInput(engine) {
                    detectTapGestures { tap ->
                        renderer.touchedAt = System.nanoTime()
                        val point = camera.pick(tap.x, tap.y, size.width.toFloat(), size.height.toFloat())
                        val world = worldToCell(point)
                        val cell = if (settings.mode == FieldMode.Torus) torusCell(world, engine.n) else world
                        engine.spark(cell)
                    }
                },
            onFrame = renderer::draw,
        )

        if (computeLink is GlLinkState.Failed) Unsupported()

        Hud(
            generation = generation,
            population = population,
            paused = paused,
            onBack = onBack,
            onPause = { paused = !paused },
            onRestart = {
                engine.start()
                generation = 0
                population = engine.latest.population
            },
        )
        AnimatedVisibility(
            visible = hintVisible,
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
        ) {
            Text(
                text = stringResource(
                    if (settings.mode == FieldMode.Torus) R.string.gesture_hint_torus else R.string.gesture_hint_infinite,
                ),
                color = LifeColors.Text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LifeColors.Panel)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Hud(
    generation: Int,
    population: Int,
    paused: Boolean,
    onBack: () -> Unit,
    onPause: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundButton("←", stringResource(R.string.back), onBack)
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(LifeColors.Panel)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.generation, generation),
                    color = LifeColors.Text,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.population, population),
                    color = lerp(MaterialTheme.colorScheme.primary, Color.White, 0.45f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            RoundButton(
                glyph = if (paused) "▶" else "❚❚",
                description = stringResource(if (paused) R.string.resume else R.string.pause),
                onClick = onPause,
            )
            RoundButton("↻", stringResource(R.string.restart), onRestart)
        }
    }
}

@Composable
private fun RoundButton(glyph: String, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(LifeColors.Panel)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Unsupported() {
    Box(Modifier.fillMaxSize().background(LifeColors.Background), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.needs_gles31),
            color = LifeColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

private const val HINT_MILLIS = 5_000L
