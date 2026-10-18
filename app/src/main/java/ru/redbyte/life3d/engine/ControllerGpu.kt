package ru.redbyte.life3d.engine

import ru.redbyte.life3d.gl.LifeCompute
import ru.redbyte.redbytefx.gl.compose.GlController

/**
 * [LifeGpu] on a RedByteFX [GlController] hosting [compute].
 *
 * Every call only queues work for the compute view's GL thread: uniform writes are applied before
 * the queued dispatch, and the read back runs after it, in the order they were made.
 */
class ControllerGpu(
    private val controller: GlController,
    private val compute: LifeCompute,
) : LifeGpu {
    override fun size(n: Int) = controller.set(compute.size, n)

    override fun wrap(torus: Boolean) = controller.set(compute.torus, if (torus) 1 else 0)

    override fun shift(x: Int, y: Int, z: Int) {
        controller.set(compute.shiftX, x)
        controller.set(compute.shiftY, y)
        controller.set(compute.shiftZ, z)
    }

    override fun rules(values: FloatArray) = controller.set(compute.rules, values)

    override fun upload(world: FloatArray) = controller.set(compute.world, world)

    override fun offsets(read: Int, write: Int) {
        controller.set(compute.readOffset, read)
        controller.set(compute.writeOffset, write)
    }

    override fun spark(x: Float, y: Float, z: Float, radius: Float, seed: Float) {
        controller.set(compute.sparkCenter, x, y, z)
        controller.set(compute.sparkRadius, radius)
        controller.set(compute.sparkSeed, seed)
    }

    override fun dispatch(groups: Int) = controller.dispatch(groups, groups, groups)

    override fun readBack(into: FloatArray, done: () -> Unit) = controller.read(compute.world, into) { done() }
}
