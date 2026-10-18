package ru.redbyte.life3d.gl

import ru.redbyte.life3d.domain.CELL_SIZE
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
import ru.redbyte.redbytefx.distance
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

class LifeScene(
    val program: ShaderProgram,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val eye: Uniform<Vec3<Flt<High>>>,
    val light: Uniform<Vec3<Flt<High>>>,
    val cellColor: Uniform<Vec3<Flt<High>>>,
    val phase: Uniform<Flt<High>>,
    val time: Uniform<Flt<High>>,
    val aspect: Uniform<Flt<High>>,

    val size: Uniform<Flt<High>>,

    val torus: Uniform<Flt<High>>,

    val decay: Uniform<Flt<High>>,

    val origin: Uniform<Vec3<Flt<High>>>,

    val viewCentre: Uniform<Vec3<Flt<High>>>,
    val viewRadius: Uniform<Flt<High>>,

    val fogNear: Uniform<Flt<High>>,
    val fogFar: Uniform<Flt<High>>,
    val target: Uniform<Vec3<Flt<High>>>,
)

const val BACKGROUND_MARK: Float = -1000f

fun backgroundModel(): FloatArray = FloatArray(INSTANCE_FLOATS).also { it[3] = BACKGROUND_MARK }

@Suppress("LongMethod")
fun lifeScene(): LifeScene {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var eye: Uniform<Vec3<Flt<High>>>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var cellColor: Uniform<Vec3<Flt<High>>>
    lateinit var phase: Uniform<Flt<High>>
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var size: Uniform<Flt<High>>
    lateinit var torus: Uniform<Flt<High>>
    lateinit var decay: Uniform<Flt<High>>
    lateinit var origin: Uniform<Vec3<Flt<High>>>
    lateinit var viewCentre: Uniform<Vec3<Flt<High>>>
    lateinit var viewRadius: Uniform<Flt<High>>
    lateinit var fogNear: Uniform<Flt<High>>
    lateinit var fogFar: Uniform<Flt<High>>
    lateinit var target: Uniform<Vec3<Flt<High>>>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        eye = uniformVec3("eye", 0f, 0f, 3f)
        light = uniformVec3("light", 0.4f, 0.8f, 0.45f)
        cellColor = uniformVec3("cell_color", 0.25f, 0.95f, 0.78f)
        phase = uniform("phase", 1f)
        time = uniform("time", 0f)
        aspect = uniform("aspect", 1f)
        size = uniform("size", 48f)
        torus = uniform("torus", 1f)
        decay = uniform("decay", 0f)
        origin = uniformVec3("origin", 0f, 0f, 0f)
        viewCentre = uniformVec3("view_centre", 0f, 0f, 0f)
        viewRadius = uniform("view_radius", 30f)
        fogNear = uniform("fog_near", 1f)
        fogFar = uniform("fog_far", 4f)
        target = uniformVec3("target", 0f, 0f, 0f)

        val normal = varyingVec3("normal")
        val world = varyingVec3("world")
        val state = varyingFloat("state")
        val screen = varyingVec2("screen")

        vertex {
            val position = attributeVec3("position")
            val surface = attributeVec3("normal")
            val column0 = attributeVec4("model0")
            val background = column0.w lt -500f
            val cell = vec3(column0.x, column0.y, column0.z)
            val value = column0.w
            val n = size.expr

            val grow = smoothstep(0f, 1f, phase.expr)
            val k = -value
            val steps = max(decay.expr, 1f)
            val ghostNow = k / steps * GHOST_SCALE
            val ghostBefore = ifElse(k gt decay.expr - 0.5f, 1f.lit, (k + 1f) / steps * GHOST_SCALE)
            val fadingLife = ifElse(decay.expr gt 0.5f, mix(ghostBefore, ghostNow, grow), 1f - grow)
            val life = ifElse(value lt -0.5f, fadingLife, ifElse(value lt 1.5f, grow, 1f.lit))

            val fromCentre = distance(cell, viewCentre.expr)
            val viewFade = smoothstep(viewRadius.expr, viewRadius.expr - 5f, fromCentre)
            val window = cell - origin.expr
            val inner = min(window + 0.5f, vec3(n, n, n) - window - 0.5f)
            val nearestWall = min(min(inner.x, inner.y), inner.z)
            val wallFade = ifElse(torus.expr gt 0.5f, 1f.lit, smoothstep(-0.1f, 4f, nearestWall))

            val beat = dot(cell, vec3(1.7f.lit, 2.3f.lit, 3.1f.lit))
            val wobble = 1f + sin(time.expr * 2.6f + position.y * 5f + beat) * 0.09f
            val radius = life * viewFade * wallFade * (CELL_SIZE * 0.5f * CELL_FILL)
            val placed = (cell + 0.5f) * CELL_SIZE + position * (radius * wobble)

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

            val uv = screen.expr
            val parallax = vec2(target.expr.x - target.expr.z * 0.5f, target.expr.y) * 0.6f
            val body = liquidFlow(vec2(uv.x * aspect.expr, uv.y) * 2.6f + parallax, time.expr)
            val folds = 1f - smoothstep(0f, 0.05f, abs(body - 0.5f))
            val centred = uv - 0.5f
            val vignette = 1f - dot(centred, centred) * 1.4f
            val liquid = (mix(deep, shallow, smoothstep(0.1f, 0.9f, body)) + tint * folds * 0.1f) * vignette

            val n = normalize(normal.expr)
            val toEye = normalize(eye.expr - world.expr)
            val lightDir = normalize(light.expr)
            val diffuse = wrapLambert(n, lightDir, 0.3f)
            val rim = fresnel(n, toEye, 2.4f)
            val shine = pow(max(dot(reflect(-lightDir, n), toEye), 0f), 28f)
            val age = saturate((state.expr - 1f) / 40f)
            val aged = mix(tint, tint * 0.5f + vec3(0.32f.lit, 0.12f.lit, 0.5f.lit) * 0.5f, age)
            val glow = mix(tint, vec3(1f.lit, 1f.lit, 1f.lit), 0.55f)

            val cooling = ifElse(
                state.expr lt -0.5f,
                0.45f + saturate(-state.expr / max(decay.expr, 1f)) * 0.35f,
                0f.lit,
            )
            val skin = mix(aged, deep, cooling)
            val rimShare = ifElse(state.expr lt -0.5f, 0.35f.lit, 0.85f.lit)
            val lit = skin * (diffuse * 0.8f + 0.2f) + glow * rim * rimShare + vec3(shine, shine, shine) * 0.45f

            val depth = length(eye.expr - world.expr)
            val murk = smoothstep(fogNear.expr, fogFar.expr, depth)
            val cellOut = mix(lit, mix(shallow, deep, 0.4f), murk)

            val rgb = ifElse(state.expr gt -500f, cellOut, liquid)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return LifeScene(
        program, view, projection, eye, light, cellColor, phase, time, aspect, size, torus, decay,
        origin, viewCentre, viewRadius, fogNear, fogFar, target,
    )
}

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

private const val CELL_FILL = 0.9f

private const val GHOST_SCALE = 0.6f
