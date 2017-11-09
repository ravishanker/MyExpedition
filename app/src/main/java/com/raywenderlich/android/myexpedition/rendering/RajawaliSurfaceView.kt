package com.raywenderlich.android.myexpedition.rendering

import android.content.Context
import android.util.AttributeSet
import com.google.atap.tangoservice.Tango
import org.rajawali3d.view.ISurface
import org.rajawali3d.view.SurfaceView

/**
 * This is a specialized `RajawaliSurfaceView` that allows rendering of a Rajawali scene
 * together with the Tango Camera Preview and optionally using the Tango Pose Estimation to drive
 * the Rajawali Camera and build Augmented Reality applications.
 */
class RajawaliView : SurfaceView {
    private lateinit var mRenderer: RajawaliRenderer

    /**
     * Get the latest camera data's timestamp. This value will be updated when
     * the updateTexture() is called.
     *
     * @return The timestamp. This can be used to associate camera data with a
     * pose or other sensor data using other pieces of the Tango API.
     */
    val timestamp: Double
        get() = mRenderer.getTimestamp()

    constructor(context: Context) : super(context) {
        // It is important to set render mode to manual to force rendering only when there is a
        // Tango Camera image available and get correct synchronization between the camera and the
        // rest of the scene.
        renderMode = ISurface.RENDERMODE_WHEN_DIRTY
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        // It is important to set render mode to manual to force rendering only when there is a
        // Tango Camera image available and get correct synchronization between the camera and the
        // rest of the scene.
        renderMode = ISurface.RENDERMODE_WHEN_DIRTY
    }

    /**
     * Sets up the `RajawaliRenderer` that will be used to render the scene.
     * In order to use Tango components, a subclass of either `TangoRajawaliRenderer`
     * or `TangoRajawaliArRenderer` should be used.
     */
    @Throws(IllegalStateException::class)
    fun setSurfaceRenderer(renderer: RajawaliRenderer) {
        super.setSurfaceRenderer(renderer)
        this.mRenderer = renderer
    }

    /**
     * Updates the TangoRajawaliView with the latest camera data. This method
     * synchronizes the data in the OpenGL context.
     *
     *
     * Call this method from the onFrameAvailable() method of
     * Tango.OnTangoUpdateListener, which provides a set of callbacks for
     * getting updates from the Project Tango sensors.
     */
    @Synchronized
    fun onFrameAvailable() {
        mRenderer.onTangoFrameAvailable()
        requestRender()
    }

    /**
     * Gets a textureId from a valid OpenGL Context through Rajawali and connects it to the
     * TangoRajawaliView.
     *
     *
     * Use OnFrameAvailable events or updateTexture calls to update the view with
     * the latest camera data. Only the RGB and fisheye cameras are currently
     * supported.
     *
     * @param tango    A reference to the Tango service.
     * @param cameraId The id of the camera to connect to. Ids listed in
     * TangoCameraIntrinsics.
     */
    fun connectToTangoCamera(tango: Tango, cameraId: Int) {
        mRenderer.connectCamera(tango, cameraId)
    }

    /**
     * Disables rendering of the Tango camera in the TangoCameraMaterial.
     * Should be called before disconnecting from the Tango service.
     */
    fun disconnectCamera() {
        mRenderer.disconnectCamera()
    }

    companion object {
        private val TAG = "RajawaliSurfaceView"
    }
}

