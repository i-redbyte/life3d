package ru.redbyte.life3d

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.redbyte.life3d.domain.CpuLife
import ru.redbyte.life3d.domain.FieldMode
import ru.redbyte.life3d.domain.LifeRule
import ru.redbyte.life3d.domain.Rule
import ru.redbyte.life3d.domain.Seeder
import ru.redbyte.life3d.gl.WORKGROUP_EDGE
import ru.redbyte.life3d.gl.lifeCompute
import ru.redbyte.redbytefx.gl.Gles30Device
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class GpuMatchesCpuTest {
    private val n = 16
    private val total = n * n * n

    @Test
    fun everyRuleMatchesTheCpuReferenceOnATorus() = onEs31 { runtime ->
        for (preset in LifeRule.entries) check(runtime, preset, FieldMode.Torus, intArrayOf(0, 0, 0))
    }

    @Test
    fun anInfiniteFieldMatchesTheCpuReferenceWithAndWithoutAShift() = onEs31 { runtime ->
        check(runtime, LifeRule.Amoeba, FieldMode.Infinite, intArrayOf(0, 0, 0))
        check(runtime, LifeRule.Reef, FieldMode.Infinite, intArrayOf(3, -2, 1))
        check(runtime, LifeRule.Lava, FieldMode.Infinite, intArrayOf(-4, 4, -4))
    }

    @Test
    fun refractoryStatesMatchTheCpuReferenceForEveryNumberOfStates() = onEs31 { runtime ->
        for (states in listOf(2, 3, 5, 10)) {
            val rule = Rule(birth = setOf(4, 6, 7, 8), survive = setOf(4, 5, 6, 7), states = states)
            check(runtime, LifeRule.Lava, FieldMode.Torus, intArrayOf(0, 0, 0), rule)
            check(runtime, LifeRule.Lava, FieldMode.Infinite, intArrayOf(2, 0, -3), rule)
        }
    }

    private fun check(
        runtime: Pair<GlProgramRuntime, ru.redbyte.life3d.gl.LifeCompute>,
        preset: LifeRule,
        mode: FieldMode,
        shift: IntArray,
        rule: Rule = preset.rule,
    ) {
        val (gpu, compute) = runtime
        val seed = Seeder.seed(n, cells = 900, density = preset.seedDensity, random = Random(preset.ordinal + 11))

        for (i in seed.indices) {
            if (seed[i] > 0f && i % 3 == 0) seed[i] = 7f
            if (seed[i] == 0f && i % 7 == 0 && rule.decay > 0) seed[i] = -(1 + i % rule.decay).toFloat()
        }
        val world = FloatArray(2 * total)
        seed.copyInto(world)
        gpu.set(compute.world, world)
        gpu.set(compute.rules, rule.uniformValues())
        gpu.set(compute.decay, rule.decay)
        gpu.set(compute.size, n)
        gpu.set(compute.readOffset, 0)
        gpu.set(compute.writeOffset, total)
        gpu.set(compute.torus, if (mode == FieldMode.Torus) 1 else 0)
        gpu.set(compute.shiftX, shift[0])
        gpu.set(compute.shiftY, shift[1])
        gpu.set(compute.shiftZ, shift[2])
        gpu.set(compute.sparkRadius, 0f)
        val groups = n / WORKGROUP_EDGE
        gpu.dispatch(groups, groups, groups)
        val back = FloatArray(2 * total)
        gpu.read(compute.world, back)

        val expected = CpuLife.step(seed, n, rule, mode, shift)
        assertArrayEquals("${rule.notation} $mode ${shift.toList()}", expected, back.copyOfRange(total, 2 * total), 0f)
    }

    private fun onEs31(block: (Pair<GlProgramRuntime, ru.redbyte.life3d.gl.LifeCompute>) -> Unit) {
        val egl = Es31Pbuffer()
        assumeTrue("needs OpenGL ES 3.1", egl.open())
        try {
            val compute = lifeCompute()
            val runtime = GlProgramRuntime(compute.program, Gles30Device())
            runtime.link()
            try {
                block(runtime to compute)
            } finally {
                runtime.destroy()
            }
        } finally {
            egl.close()
        }
    }
}

private class Es31Pbuffer {
    private var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var context: EGLContext = EGL14.EGL_NO_CONTEXT
    private var surface: EGLSurface = EGL14.EGL_NO_SURFACE

    fun open(): Boolean {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        if (!EGL14.eglInitialize(display, version, 0, version, 1)) return false
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        val attributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE,
        )
        if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) || count[0] == 0) return false
        val config = configs[0]
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL_CONTEXT_MINOR_VERSION, 1, EGL14.EGL_NONE),
            0,
        )
        if (context == EGL14.EGL_NO_CONTEXT) return false
        surface = EGL14.eglCreatePbufferSurface(display, config, intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0)
        return EGL14.eglMakeCurrent(display, surface, surface, context)
    }

    fun close() {
        if (display == EGL14.EGL_NO_DISPLAY) return
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }

    private companion object {
        const val EGL_OPENGL_ES3_BIT = 0x40
        const val EGL_CONTEXT_MINOR_VERSION = 0x30FB
    }
}
