package com.raywenderlich.android.myexpedition.renderables

import android.graphics.Color
import com.raywenderlich.android.myexpedition.renderables.primitives.Points
import org.rajawali3d.materials.Material
import java.nio.FloatBuffer


/**
 * Renders a point cloud using colors to indicate distance to the depth sensor.
 * Coloring is based on the light spectrum: closest points are in red, farthest in violet.
 */
class PointCloud(maxPoints: Int) : Points(maxPoints, true) {

    private val mColorArray: FloatArray
    private val mPalette: IntArray

    init {
        mPalette = createPalette()
        mColorArray = FloatArray(maxPoints * 4)
        val m = Material()
        m.useVertexColors(true)
        material = m
    }

    /**
     * Pre-calculate a palette to be used to translate between point distance and RGB color.
     */
    private fun createPalette(): IntArray {
        val palette = IntArray(PALETTE_SIZE)
        val hsv = FloatArray(3)
        hsv[2] = 1f
        hsv[1] = hsv[2]
        for (i in 0 until PALETTE_SIZE) {
            hsv[0] = (HUE_END - HUE_BEGIN) * i / PALETTE_SIZE + HUE_BEGIN
            palette[i] = Color.HSVToColor(hsv)
        }
        return palette
    }

    /**
     * Calculate the right color for each point in the point cloud.
     */
    private fun calculateColors(pointCount: Int, pointCloudBuffer: FloatBuffer) {
        val points = FloatArray(pointCount * 3)
        pointCloudBuffer.rewind()
        pointCloudBuffer.get(points)
        pointCloudBuffer.rewind()

        var color: Int
        var colorIndex: Int
        var z: Float
        for (i in 0 until pointCount) {
            z = points[i * 3 + 2]
            colorIndex = Math.min(z / CLOUD_MAX_Z * mPalette.size, (mPalette.size - 1).toFloat()).toInt()
            color = mPalette[colorIndex]
            mColorArray[i * 4] = Color.red(color) / 255f
            mColorArray[i * 4 + 1] = Color.green(color) / 255f
            mColorArray[i * 4 + 2] = Color.blue(color) / 255f
            mColorArray[i * 4 + 3] = Color.alpha(color) / 255f
        }
    }

    /**
     * Update the points and colors in the point cloud.
     */
    fun updateCloud(pointCount: Int, pointBuffer: FloatBuffer) {
        calculateColors(pointCount, pointBuffer)
        updatePoints(pointCount, pointBuffer, mColorArray)
    }

    companion object {
        // Maximum depth range used to calculate coloring (min = 0)
        val CLOUD_MAX_Z = 5f
        val PALETTE_SIZE = 360
        val HUE_BEGIN = 0f
        val HUE_END = 320f
    }
}
