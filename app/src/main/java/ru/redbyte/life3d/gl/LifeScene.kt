package ru.redbyte.life3d.gl

import ru.redbyte.life3d.domain.INSTANCE_FLOATS
import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.dot
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.min
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.mod
import ru.redbyte.redbytefx.normalize
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.pow
import ru.redbyte.redbytefx.reflect
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.smoothstep
import ru.redbyte.redbytefx.stdlib.fresnel
import ru.redbyte.redbytefx.stdlib.wrapLambert
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.w
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

/**
 * The render program and its uniforms. One GLES 3.0 program draws two things:
 *
 * - the liquid the cells live in: a full-screen quad, marked by [BACKGROUND_MARK] in the
 *   instance's fourth float, shaded with a domain-warped fBm from the standard library;
 * - the cells: one instanced sphere per visible cell, whose first matrix column carries
 *   `(x, y, z, value)` from [ru.redbyte.life3d.domain.InstancePacker].
 */
class LifeScene(
    val program: ShaderProgram,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val eye: Uniform<Vec3<Flt<High>>>,
    val light: Uniform<Vec3<Flt<High>>>,
    val cellColor: Uniform<Vec3<Flt<High>>>,
    val size: Uniform<Flt<High>>,
    val drift: Uniform<Vec3<Flt<High>>>,
    val phase: Uniform<Flt<High>>,
    val time: Uniform<Flt<High>>,
    val aspect: Uniform<Flt<High>>,
    val torus: Uniform<Flt<High>>,
    val origin: Uniform<Vec3<Flt<High>>>,
    val target: Uniform<Vec3<Flt<High>>>,
    val focus: Uniform<Flt<High>>,
)

/** Fourth float of the background quad's model column: no cell has a value this low. */
const val BACKGROUND_MARK: Float = -10f

/** Model matrix for the background draw: only the mark matters. */
fun backgroundModel(): FloatArray = FloatArray(INSTANCE_FLOATS).also { it[3] = BACKGROUND_MARK }

@Suppress("LongMethod")
fun lifeScene(): LifeScene {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var eye: Uniform<Vec3<Flt<High>>>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var cellColor: Uniform<Vec3<Flt<High>>>
    lateinit var size: Uniform<Flt<High>>
    lateinit var drift: Uniform<Vec3<Flt<High>>>
    lateinit var phase: Uniform<Flt<High>>
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var torus: Uniform<Flt<High>>
    lateinit var origin: Uniform<Vec3<Flt<High>>>
    lateinit var target: Uniform<Vec3<Flt<High>>>
    lateinit var focus: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        eye = uniformVec3("eye", 0f, 0f, 3f)
        light = uniformVec3("light", 0.4f, 0.8f, 0.45f)
        cellColor = uniformVec3("cell_color", 0.25f, 0.95f, 0.78f)
        size = uniform("size", 24f)
        drift = uniformVec3("drift", 0f, 0f, 0f)
        phase = uniform("phase", 1f)
        time = uniform("time", 0f)
        aspect = uniform("aspect", 1f)
        torus = uniform("torus", 1f)
        origin = uniformVec3("origin", 0f, 0f, 0f)
        target = uniformVec3("target", 0f, 0f, 0f)
        focus = uniform("focus", 3.8f)

        val normal = varyingVec3("normal")
        val world = varyingVec3("world")
        val state = varyingFloat("state")
        val screen = varyingVec2("screen")

        vertex {
            val position = attributeVec3("position")
            val surface = attributeVec3("normal")
            val column0 = attributeVec4("model0")
            val background = column0.w lt -5f
            val cell = vec3(column0.x, column0.y, column0.z)
            val n = size.expr

            // On a torus, drifting cells leave one face and come back through the other. In an
            // infinite field the cell sits at its world place: window origin plus window cell.
            val onTorus = torus.expr gt 0.5f
            val window = ifElse(onTorus, mod(cell + drift.expr, n), cell)
            val worldCell = ifElse(onTorus, window, cell + origin.expr)
            val centre = (worldCell + 0.5f) / n * 2f - 1f
            // Cells near a face of the window shrink away: across a torus wall, or at the edge of
            // the simulated part of an infinite world.
            val inner = min(window + 0.5f, vec3(n, n, n) - window - 0.5f)
            val nearestFace = min(min(inner.x, inner.y), inner.z)
            // A torus wall is crossed in one cell; the edge of an infinite field's window is softened
            // over a few cells, so a colony pressing against it looks rounded rather than boxed.
            val softness = ifElse(onTorus, 0.6f.lit, 3.5f.lit)
            val edgeFade = smoothstep(-0.1f.lit, softness, nearestFace)

            // Newborn cells grow and dying cells shrink over one generation.
            val value = column0.w
            val grow = smoothstep(0f, 1f, phase.expr)
            val life = ifElse(value lt -0.5f, 1f - grow, ifElse(value lt 1.5f, grow, 1f.lit))

            // A slow jelly wobble, out of step from cell to cell.
            val beat = dot(cell, vec3(1.7f.lit, 2.3f.lit, 3.1f.lit))
            val wobble = 1f + sin(time.expr * 2.6f + position.y * 5f + beat) * 0.09f
            val radius = 0.86f / n * life * edgeFade
            val placed = centre + position * (radius * wobble)

            normal.set(surface)
            world.set(placed)
            state.set(value)
            screen.set(vec2(position.x, position.y) * 0.5f + 0.5f)
            val scene = projection.expr * (view.expr * vec4(placed.x, placed.y, placed.z, 1f.lit))
            glPosition(ifElse(background, vec4(position.x, position.y, 0.9999f.lit, 1f.lit), scene))
        }

        fragment {
            val tint = cellColor.expr
            val deep = tint * 0.06f + vec3(0.008f.lit, 0.014f.lit, 0.03f.lit)
            val shallow = tint * 0.24f + vec3(0.02f.lit, 0.035f.lit, 0.06f.lit)

            // The liquid: sines folded into each other. Hash noise built on fract(sin(big)) turns
            // blocky on mobile GPUs, whose sin loses precision far from zero; these stay smooth.
            val uv = screen.expr
            // The liquid drifts a little against camera moves, so travelling through it is felt.
            val parallax = vec2(target.expr.x - target.expr.z * 0.5f, target.expr.y) * 0.6f
            val body = liquidFlow(vec2(uv.x * aspect.expr, uv.y) * 2.6f + parallax, time.expr)
            val folds = 1f - smoothstep(0f, 0.05f, abs(body - 0.5f))
            val centred = uv - 0.5f
            val vignette = 1f - dot(centred, centred) * 1.4f
            val liquid = (mix(deep, shallow, smoothstep(0.1f, 0.9f, body)) + tint * folds * 0.1f) * vignette

            // The cells: soft wrapped diffuse, a glowing rim, and a wet highlight.
            val n = normalize(normal.expr)
            val toEye = normalize(eye.expr - world.expr)
            val lightDir = normalize(light.expr)
            val diffuse = wrapLambert(n, lightDir, 0.3f)
            val rim = fresnel(n, toEye, 2.4f)
            val shine = pow(max(dot(reflect(-lightDir, n), toEye), 0f), 28f)
            val age = saturate((state.expr - 1f) / 40f)
            val aged = mix(tint, tint * 0.5f + vec3(0.32f.lit, 0.12f.lit, 0.5f.lit) * 0.5f, age)
            val glow = mix(tint, vec3(1f.lit, 1f.lit, 1f.lit), 0.55f)
            val dying = state.expr lt -0.5f
            val skin = ifElse(dying, mix(aged, deep, 0.55f), aged)
            val lit = skin * (diffuse * 0.8f + 0.2f) + glow * rim * 0.85f + vec3(shine, shine, shine) * 0.45f

            // Cells further into the liquid fade into it.
            val depth = length(eye.expr - world.expr)
            val murk = smoothstep(focus.expr - 0.7f, focus.expr + 1.5f, depth) * 0.65f
            val cellColorOut = mix(lit, shallow, murk)

            val rgb = ifElse(state.expr gt -5f, cellColorOut, liquid)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return LifeScene(
        program, view, projection, eye, light, cellColor, size, drift, phase, time, aspect,
        torus, origin, target, focus,
    )
}

/**
 * A slowly moving liquid in `0..1`: each pass pushes the point along sines of the other axis,
 * so the folds bend into each other like stirred syrup.
 */
private fun liquidFlow(point: Expr<Vec2<Flt<High>>>, time: Expr<Flt<High>>): Expr<Flt<High>> {
    var q = point
    for (pass in 1..LIQUID_PASSES) {
        val k = pass.toFloat()
        val push = vec2(
            sin(q.y * (0.9f + 0.2f * k) + time * 0.23f + k),
            cos(q.x * (0.8f + 0.25f * k) - time * 0.19f + k * 1.7f),
        )
        q = q + push * (0.55f / k)
    }
    return sin(q.x * 1.3f + q.y * 0.7f) * 0.5f + 0.5f
}

private const val LIQUID_PASSES = 5
