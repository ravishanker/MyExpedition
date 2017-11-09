package com.raywenderlich.android.myexpedition.rendering

import android.content.Context
import android.util.Log
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoInvalidException
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.materials.textures.StreamingTexture
import org.rajawali3d.math.Matrix4
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.renderer.Renderer
import javax.microedition.khronos.opengles.GL10

/**
 * This is a specialization of `RajawaliRenderer` that makes it easy to build
 * augmented reality applications.
 * It sets up a simple scene with the camera contents rendered in full screen in the background and
 * a default Rajawali `Camera` automatically adjusted to replicate the real world
 * movement of the Tango device in the virtual world.
 *
 *
 * It is used the same as any `RajawaliRenderer` with the following additional
 * considerations:
 *
 *
 * - It is optional (although recommended) to overwrite the `initScene` method. If this
 * method is overwritten, it is important to call `super.initScene()` at the beginning
 * of the overwriting method.
 * - It is important to be careful to not clear the scene since this will remove the Tango camera
 * from the background.
 * - In most cases the Rajawali camera will not be handled by the user since it is automatically
 * handled by this mRenderer.
 */
abstract class RajawaliRenderer(context: Context) : Renderer(context) {

    // Rajawali scene objects to render the color camera
    private var mTangoCameraTexture =  StreamingTexture("camera", null as StreamingTexture.ISurfaceListener?)
    private var mBackgroundQuad = ScreenQuad()

    private var mTango: Tango? = null
    private var mCameraId: Int = 0
    private var mUpdatePending = false
    private var mConnectedTextureId = -1
    private var mLastRGBFrameTimestamp = -1.0
    private var mIsCameraConfigured = false
    private var mProjectionMatrix: Matrix4? = null


    /**
     * Sets up the initial scene with a default Rajawali camera and a background quad rendering
     * the Tango camera contents.
     */
    override fun initScene() {
        mBackgroundQuad = ScreenQuad()
        mTangoCameraTexture = StreamingTexture("camera", null as StreamingTexture.ISurfaceListener?)

        val tangoCameraMaterial = Material()
        tangoCameraMaterial.colorInfluence = 0f
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture)
            mBackgroundQuad.material = tangoCameraMaterial
        } catch (e: ATexture.TextureException) {
            e.printStackTrace()
        }

        currentScene.addChildAt(mBackgroundQuad, 0)
    }

    override fun onRender(elapsedRealTime: Long, deltaTime: Double) {
        synchronized(this) {
            // mTango != null is used to indicate that a Tango device is connected to this
            // renderer, via a corresponding TangoRajawaliView
            if (mTango != null) {
                try {
                    if (mUpdatePending) {
                        mLastRGBFrameTimestamp = updateTexture()
                        mUpdatePending = false
                    }
                    if (!mIsCameraConfigured) {
                        currentCamera.projectionMatrix = mProjectionMatrix
                        mIsCameraConfigured = true
                    } else {

                    }
                } catch (ex: TangoInvalidException) {
                    Log.e(TAG, "Error while updating texture!", ex)
                } catch (ex: TangoErrorException) {
                    Log.e(TAG, "Error while updating texture!", ex)
                }

            }
        }

        super.onRender(elapsedRealTime, deltaTime)
    }

    /**
     * Updates the TangoCameraMaterial with the latest camera data.
     *
     * @return the timestamp of the RGB image rendered into the texture.
     */
    private fun updateTexture(): Double {
        // Try this again here because it is possible that when the user called
        // connectToTangoCamera the texture wasn't assigned yet and the connection couldn't
        // be done
        if (mConnectedTextureId != getTextureId()) {
            mConnectedTextureId = connectTangoTexture()
        }
        return if (mConnectedTextureId != -1) {
            // Copy the camera frame from the camera to the OpenGL texture
            mTango!!.updateTexture(this.mCameraId)
        } else -1.0
    }

    private fun getTextureId(): Int {
        return if (mTangoCameraTexture == null) -1 else mTangoCameraTexture.textureId
    }

    private fun connectTangoTexture(): Int {
        var textureId = -1
        if (mTangoCameraTexture != null) {
            textureId = mTangoCameraTexture.textureId
        }
        mTango?.connectTextureId(mCameraId, textureId)
        return textureId
    }

    /**
     * Intended to be called from `TangoRajawaliView`.
     */
    @Synchronized internal fun connectCamera(tango: Tango, cameraId: Int) {
        mTango = tango
        mCameraId = cameraId
        if (mTango != null) {
            mConnectedTextureId = connectTangoTexture()
        }
        val intrinsics = tango.getCameraIntrinsics(mCameraId)
        mProjectionMatrix = ScenePoseCalculator.calculateProjectionMatrix(
                intrinsics.width, intrinsics.height,
                intrinsics.fx, intrinsics.fy, intrinsics.cx, intrinsics.cy)
    }

    /**
     * Intended to be called from `TangoRajawaliView`.
     */
    @Synchronized internal fun disconnectCamera() {
        val oldTango = mTango
//        mTango = null
        oldTango?.disconnectCamera(mCameraId)
        mConnectedTextureId = -1
        mIsCameraConfigured = false
    }

    /**
     * Intended to be called from `TangoRajawaliView`.
     */
    @Synchronized internal fun onTangoFrameAvailable() {
        mUpdatePending = true
    }

    /**
     * Get the latest camera frame timestamp. This value will be updated when
     * the updateTexture() is called.
     *
     * @return The timestamp. This can be used to associate camera data with a
     * pose or other sensor data using other pieces of the Tango API.
     */
    @Synchronized
    fun getTimestamp(): Double {
        return mLastRGBFrameTimestamp
    }

    override fun onRenderSurfaceSizeChanged(gl: GL10, width: Int, height: Int) {
        super.onRenderSurfaceSizeChanged(gl, width, height)
        // The camera projection matrix gets reset whenever the render surface is changed
        mIsCameraConfigured = false
    }

    companion object {
        private val TAG = RajawaliRenderer::class.java.simpleName
    }
}
