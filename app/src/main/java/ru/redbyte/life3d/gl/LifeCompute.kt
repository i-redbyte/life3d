package ru.redbyte.life3d.gl

import ru.redbyte.life3d.domain.ALIVE
import ru.redbyte.life3d.domain.DYING
import ru.redbyte.life3d.domain.MAX_AGE
import ru.redbyte.life3d.domain.NEIGHBOUR_COUNTS
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.StorageArray
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.dot
import ru.redbyte.redbytefx.fract
import ru.redbyte.redbytefx.ge
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.intLit
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.min
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.or
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.toFloat
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

/** Each workgroup handles a 4×4×4 brick of cells, so the field edge must be a multiple of 4. */
const val WORKGROUP_EDGE: Int = 4

/**
 * One generation of 3D Life as a GLES 3.1 compute shader, with the handles the game writes.
 *
 * The world is one storage array holding two grids. Each dispatch reads the grid at [readOffset]
 * and writes the other one at [writeOffset]; the game swaps the offsets every generation, so the
 * cells never leave the GPU between generations and the CPU only reads the result back to draw it.
 */
class LifeCompute(
    val program: ShaderProgram,
    val world: StorageBlock,
    val size: Uniform<IntS>,
    val readOffset: Uniform<IntS>,
    val writeOffset: Uniform<IntS>,
    val torus: Uniform<IntS>,
    val shiftX: Uniform<IntS>,
    val shiftY: Uniform<IntS>,
    val shiftZ: Uniform<IntS>,
    val rules: UniformBlock,
    val sparkCenter: Uniform<Vec3<Flt<High>>>,
    val sparkRadius: Uniform<Flt<High>>,
    val sparkSeed: Uniform<Flt<High>>,
)

fun lifeCompute(): LifeCompute {
    lateinit var world: StorageBlock
    lateinit var size: Uniform<IntS>
    lateinit var readOffset: Uniform<IntS>
    lateinit var writeOffset: Uniform<IntS>
    lateinit var torus: Uniform<IntS>
    lateinit var shiftX: Uniform<IntS>
    lateinit var shiftY: Uniform<IntS>
    lateinit var shiftZ: Uniform<IntS>
    lateinit var rules: UniformBlock
    lateinit var sparkCenter: Uniform<Vec3<Flt<High>>>
    lateinit var sparkRadius: Uniform<Flt<High>>
    lateinit var sparkSeed: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles31) {
        size = uniformInt("size", 0)
        readOffset = uniformInt("read_offset", 0)
        writeOffset = uniformInt("write_offset", 0)
        torus = uniformInt("torus", 1)
        shiftX = uniformInt("shift_x", 0)
        shiftY = uniformInt("shift_y", 0)
        shiftZ = uniformInt("shift_z", 0)
        sparkCenter = uniformVec3("spark_center", 0f, 0f, 0f)
        sparkRadius = uniform("spark_radius", 0f)
        sparkSeed = uniform("spark_seed", 0f)

        lateinit var birth: StorageArray<Flt<High>>
        lateinit var survive: StorageArray<Flt<High>>
        rules = uniformBlock("rules") {
            birth = floatArray("birth", NEIGHBOUR_COUNTS)
            survive = floatArray("survive", NEIGHBOUR_COUNTS)
        }
        lateinit var cells: StorageArray<Flt<High>>
        world = storageBlock("world") {
            cells = floatArray("cells")
        }

        compute(WORKGROUP_EDGE, WORKGROUP_EDGE, WORKGROUP_EDGE) {
            val n = size.expr
            val wraps = torus.expr gt 0.intLit
            // A neighbour is at most one step and one window shift away, so wrapping the torus is a
            // single comparison on each side; GLSL has no integer modulo that wraps negatives.
            val wrap = fn(0.intLit, 0.intLit, name = "wrap") { value, edge ->
                ifElse(value lt 0.intLit, value + edge, ifElse(value ge edge, value - edge, value))
            }
            // In an open field nothing lives beyond the walls: clamp the index to stay in bounds,
            // and count the cell only when the coordinate was really inside.
            val inside = fn(0.intLit, 0.intLit, name = "inside") { value, edge ->
                ifElse((value ge 0.intLit) and (value lt edge), 1f.lit, 0f.lit)
            }
            val clampCell = fn(0.intLit, 0.intLit, name = "clamp_cell") { value, edge ->
                ifElse(value lt 0.intLit, 0.intLit, ifElse(value ge edge, edge - 1.intLit, value))
            }
            val read = fn(0.intLit, 0.intLit, 0.intLit, name = "read_cell") { cx, cy, cz ->
                val wx = ifElse(wraps, wrap(cx, n), clampCell(cx, n))
                val wy = ifElse(wraps, wrap(cy, n), clampCell(cy, n))
                val wz = ifElse(wraps, wrap(cz, n), clampCell(cz, n))
                val present = ifElse(wraps, 1f.lit, inside(cx, n) * inside(cy, n) * inside(cz, n))
                cells[readOffset.expr + (wz * n + wy) * n + wx] * present
            }
            val x = globalId.x
            val y = globalId.y
            val z = globalId.z
            val here = (z * n + y) * n + x
            // The window may have moved over the world: this cell continues cell (x, y, z) + shift.
            val sx = x + shiftX.expr
            val sy = y + shiftY.expr
            val sz = z + shiftZ.expr

            // Count live cells in the 3×3×3 block, the cell itself included.
            val block = local(0.intLit, "block")
            repeat(3) { dz ->
                repeat(3) { dy ->
                    repeat(3) { dx ->
                        val neighbour = read(sx + dx - 1.intLit, sy + dy - 1.intLit, sz + dz - 1.intLit)
                        block.set(block.expr + ifElse(neighbour gt ALIVE, 1.intLit, 0.intLit))
                    }
                }
            }

            val self = local(read(sx, sy, sz), "self")
            val alive = self.expr gt ALIVE
            val neighbours = block.expr - ifElse(alive, 1.intLit, 0.intLit)
            val lives = ifElse(alive, survive[neighbours] gt 0.5f, birth[neighbours] gt 0.5f)

            // A tap seeds a random cloud of life around the spark centre, across the wrap too.
            val point = vec3(x.toFloat(), y.toFloat(), z.toFloat())
            val edge = n.toFloat()
            val apart = abs(point - sparkCenter.expr)
            val wrapped = ifElse(wraps, min(apart, vec3(edge, edge, edge) - apart), apart)
            val noise = fract(sin(dot(point, vec3(12.9898f.lit, 78.233f.lit, 37.719f.lit)) + sparkSeed.expr) * 43758.547f)
            val sparked = (length(wrapped) lt sparkRadius.expr) and (noise lt SPARK_FILL)

            val next = ifElse(
                lives or sparked,
                ifElse(alive, min(self.expr + 1f, MAX_AGE.lit), 1f.lit),
                ifElse(alive, DYING.lit, 0f.lit),
            )
            cells[writeOffset.expr + here].store(next)
        }
    }
    return LifeCompute(
        program, world, size, readOffset, writeOffset, torus, shiftX, shiftY, shiftZ,
        rules, sparkCenter, sparkRadius, sparkSeed,
    )
}

/** Share of the cells inside a spark that come alive. */
private const val SPARK_FILL = 0.42f
