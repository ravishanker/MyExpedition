package com.raywenderlich.android.myexpedition.renderables.primitives

import android.opengl.GLES10
import android.opengl.GLES20
import org.rajawali3d.Object3D
import java.nio.FloatBuffer


/**
 * A Point primitive for Rajawali.
 * Intended to be contributed and PR'ed to Rajawali.
 */
open class Points(private val mMaxNumberOfVertices: Int, isCreateColors: Boolean) : Object3D() {

    init {
        init(true, isCreateColors)
    }

    // Initialize the buffers for Points primitive.
    // Since only vertex, index and color buffers are used,
    // we only initialize them using setData call.
    private fun init(createVBOs: Boolean, createColors: Boolean) {
        val vertices = FloatArray(mMaxNumberOfVertices * 3)
        val indices = IntArray(mMaxNumberOfVertices)
        for (i in indices.indices) {
            indices[i] = i
        }
        var colors: FloatArray? = null
        if (createColors) {
            colors = FloatArray(mMaxNumberOfVertices * 4)
        }
        setData(vertices, null, null, colors, indices, true)
    }

    // Update the geometry of the points based on the provided points float buffer.
    fun updatePoints(pointCount: Int, pointCloudBuffer: FloatBuffer) {
//        mGeometry.setNumIndices(pointCount)
        mGeometry.setIndices(IntArray(pointCount))
        mGeometry.vertices = pointCloudBuffer
        mGeometry.changeBufferData(mGeometry.vertexBufferInfo, mGeometry.vertices, 0,
                pointCount * 3)
    }

    // Update the geometry of the points based on the provided points float buffer and corresponding
    // colors based on the provided float array.
    fun updatePoints(pointCount: Int, points: FloatBuffer, colors: FloatArray) {
        if (pointCount > mMaxNumberOfVertices) {
            throw RuntimeException(
                    String.format("pointClount = %d exceeds maximum number of points = %d",
                            pointCount, mMaxNumberOfVertices))
        }
//        mGeometry.setNumIndices(pointCount)
        mGeometry.setIndices(IntArray(pointCount))
        mGeometry.vertices = points
        mGeometry.changeBufferData(mGeometry.vertexBufferInfo, mGeometry.vertices, 0,
                pointCount * 3)
        mGeometry.setColors(colors)
        mGeometry.changeBufferData(mGeometry.colorBufferInfo, mGeometry.colors, 0,
                pointCount * 4)
    }

    public override fun preRender() {
        super.preRender()
        drawingMode = GLES20.GL_POINTS
        GLES10.glPointSize(5.0f)
    }
}