package com.raywenderlich.android.myexpedition.renderables

import android.graphics.Color
import org.rajawali3d.materials.Material
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import java.util.*


/**
 * A primitive which represents a combination of Frustum and Axes.
 */
class FrustumAxes(thickness: Float) : Line3D(makePoints(), thickness, makeColors()) {

    init {
        val material = Material()
        material.useVertexColors(true)
        setMaterial(material)
    }

    companion object {
        private val FRUSTUM_WIDTH = 0.8f
        private val FRUSTUM_HEIGHT = 0.6f
        private val FRUSTUM_DEPTH = 0.5f

        private fun makePoints(): Stack<Vector3> {
            val o = Vector3(0.0, 0.0, 0.0)
            val a = Vector3((-FRUSTUM_WIDTH / 2f).toDouble(), (FRUSTUM_HEIGHT / 2f).toDouble(), (-FRUSTUM_DEPTH).toDouble())
            val b = Vector3((FRUSTUM_WIDTH / 2f).toDouble(), (FRUSTUM_HEIGHT / 2f).toDouble(), (-FRUSTUM_DEPTH).toDouble())
            val c = Vector3((FRUSTUM_WIDTH / 2f).toDouble(), (-FRUSTUM_HEIGHT / 2f).toDouble(), (-FRUSTUM_DEPTH).toDouble())
            val d = Vector3((-FRUSTUM_WIDTH / 2f).toDouble(), (-FRUSTUM_HEIGHT / 2f).toDouble(), (-FRUSTUM_DEPTH).toDouble())

            val x = Vector3(1.0, 0.0, 0.0)
            val y = Vector3(0.0, 1.0, 0.0)
            val z = Vector3(0.0, 0.0, 1.0)

            val points = Stack<Vector3>()
            Collections.addAll(points, o, x, o, y, o, z, o, a, b, o, b, c, o, c, d, o, d, a)

            return points
        }

        private fun makeColors(): IntArray {
            val colors = IntArray(18)
            Arrays.fill(colors, Color.BLACK)
            colors[0] = Color.RED
            colors[1] = Color.RED
            colors[2] = Color.GREEN
            colors[3] = Color.GREEN
            colors[4] = Color.BLUE
            colors[5] = Color.BLUE
            return colors
        }
    }
}
