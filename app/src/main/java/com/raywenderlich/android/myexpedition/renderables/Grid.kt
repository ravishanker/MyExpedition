package com.raywenderlich.android.myexpedition.renderables

import android.opengl.GLES20
import org.rajawali3d.materials.Material
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.Line3D
import java.util.*


/**
 * Rajawali object which represents the 'floor' of the current scene.
 * This is a static grid placed in the scene to provide perspective in the
 * various views.
 */

class Grid(size: Int, step: Int, thickness: Float, color: Int) : Line3D() {

    init {
        Line3D(calculatePoints(size, step), thickness, color)
        val material = Material()
        material.color = color
        this.material = material
    }

    private fun calculatePoints(size: Int, step: Int): Stack<Vector3> {
        val points = Stack<Vector3>()

        // Rows
        run {
            var i = -size / 2f
            while (i <= size / 2f) {
                points.add(Vector3(i.toDouble(), 0.0, (-size / 2f).toDouble()))
                points.add(Vector3(i.toDouble(), 0.0, (size / 2f).toDouble()))
                i += step.toFloat()
            }
        }

        // Columns
        var i = -size / 2f
        while (i <= size / 2f) {
            points.add(Vector3((-size / 2f).toDouble(), 0.0, i.toDouble()))
            points.add(Vector3((size / 2f).toDouble(), 0.0, i.toDouble()))
            i += step.toFloat()
        }

        return points
    }

    override fun init(createVBOs: Boolean) {
        super.init(createVBOs)
        drawingMode = GLES20.GL_LINES
    }
}
