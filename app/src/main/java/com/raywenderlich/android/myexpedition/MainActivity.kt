package com.raywenderlich.android.myexpedition

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.atap.tangoservice.*
import com.raywenderlich.android.myexpedition.rendering.AugmentedRealityRenderer
import com.raywenderlich.android.myexpedition.rendering.DeviceExtrinsics
import com.raywenderlich.android.myexpedition.rendering.PointCloudRajawaliRenderer
import kotlinx.android.synthetic.main.activity_main.*
import org.rajawali3d.scene.ASceneFrameCallback
import org.rajawali3d.view.ISurface
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicBoolean
import com.google.atap.tangoservice.TangoInvalidException
import com.google.atap.tangoservice.TangoErrorException
import com.google.atap.tangoservice.TangoOutOfDateException
import com.google.atap.tangoservice.Tango
import com.google.atap.tangoservice.TangoConfig


class MainActivity : AppCompatActivity() {
    private val TAG = MainActivity::class.java.simpleName

    private lateinit var session: Session
    private lateinit var defaultConfig: Config

    private var textureId = -1
    private val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES

    private lateinit var mConfig: TangoConfig
    private lateinit var tango: Tango
    private val mIsFrameAvailableTangoThread = AtomicBoolean(false)
    private var isConnected = AtomicBoolean(false)
    private var mIntrinsics: TangoCameraIntrinsics? = null
    private lateinit var mExtrinsics: DeviceExtrinsics
    private var mCameraPoseTimestamp = 0.0
    val FRAME_PAIR = TangoCoordinateFramePair(
            TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
            TangoPoseData.COORDINATE_FRAME_DEVICE)

    private var loadingMessageSnackbar: Snackbar? = null
    private var displayRotation = 0

    private lateinit var arRenderer: AugmentedRealityRenderer
    private lateinit var pointCloudRenderer: PointCloudRajawaliRenderer
//    private var bgRenderer = BackgroundRenderer()
//    private lateinit var backgroundRenderer: BackgroundRajawaliRenderer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        session = Session(this)

        tango = Tango(this@MainActivity, Runnable // Pass in a Runnable to be called from UI thread when Tango is ready. This Runnable
        // will be running on a new thread.
        // When Tango is ready, we can call Tango functions safely here only when there
        // are no UI thread changes involved.
        {
            // Synchronize against disconnecting while the service is being used in the
            // OpenGL thread or in the UI thread.
            synchronized(this@MainActivity) {
                try {
                    mConfig = setupTangoConfig(tango)
                    tango.connect(mConfig)
                    startupTango()
//                    TangoSupport.initialize(mTango)
                    isConnected = AtomicBoolean(true)
                    setDisplayRotation()
                } catch (e: TangoOutOfDateException) {
                    Log.e(TAG, getString(R.string.exception_out_of_date), e)
                    showsToastAndFinishOnUiThread(R.string.exception_out_of_date)
                } catch (e: TangoErrorException) {
                    Log.e(TAG, getString(R.string.exception_tango_error), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_error)
                } catch (e: TangoInvalidException) {
                    Log.e(TAG, getString(R.string.exception_tango_invalid), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_invalid)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        })

        // Create default config, check is supported, create session from that config.
        defaultConfig = Config.createDefaultConfig()
        if (!session.isSupported(defaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Set up renderer
        arRenderer = AugmentedRealityRenderer(this)
        pointCloudRenderer = PointCloudRajawaliRenderer(this)

        surfaceview.setSurfaceRenderer(arRenderer)

//        backgroundRenderer = BackgroundRajawaliRenderer(this)

//        setupRenderer()
    }

    override fun onResume() {
        super.onResume()

        // ARCore requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            showLoadingMessage()
            // Note that order matters - see the note in onPause(), the reverse applies here.
            session.resume(defaultConfig)
            surfaceview.onResume()

            if (isConnected.compareAndSet(false, true)) {
                try {
                    bindTangoService()
                    connectRenderer()
                } catch (e: TangoOutOfDateException) {
                    Toast.makeText(applicationContext, R.string.exception_out_of_date,
                            Toast.LENGTH_SHORT).show()
                }
            }
//            session.setCameraTextureName(getTextureName())
//            backgroundRenderer.connectCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
        } else {
            CameraPermissionHelper.requestCameraPermission(this)
        }
    }

    private fun getTextureName(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(textureTarget, textureId)
        return textureId
    }

    override fun onPause() {
        super.onPause()

        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.

        if (isConnected.compareAndSet(true, false)) {
            arRenderer.currentScene.clearFrameCallbacks()
            surfaceview.disconnectCamera()
            tango.disconnect()
        }

        surfaceview.disconnectCamera()
        surfaceview.onPause()
        session.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Standard Android full-screen functionality.
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }


    /**
     * Initialize Tango Service as a normal Android Service.
     */
    private fun bindTangoService() {
        // Initialize Tango Service as a normal Android Service. Since we call mTango.disconnect()
        // in onPause, this will unbind Tango Service, so every time onResume gets called we
        // should create a new Tango object.
        tango = Tango(this@MainActivity, Runnable // Pass in a Runnable to be called from UI thread when Tango is ready; this Runnable
        // will be running on a new thread.
        // When Tango is ready, we can call Tango functions safely here only when there are no
        // UI thread changes involved.
        {
            // Synchronize against disconnecting while the service is being used in the OpenGL
            // thread or in the UI thread.
            synchronized(this@MainActivity) {
                try {
                    mConfig = setupTangoConfig(tango)
                    tango.connect(mConfig)
                    startupTango()
//                    Tango.initialize(tango)
                    isConnected = AtomicBoolean(true)
                    setDisplayRotation()
                } catch (e: TangoOutOfDateException) {
                    Log.e(TAG, getString(R.string.exception_out_of_date), e)
                    showsToastAndFinishOnUiThread(R.string.exception_out_of_date)
                } catch (e: TangoErrorException) {
                    Log.e(TAG, getString(R.string.exception_tango_error), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_error)
                } catch (e: TangoInvalidException) {
                    Log.e(TAG, getString(R.string.exception_tango_invalid), e)
                    showsToastAndFinishOnUiThread(R.string.exception_tango_invalid)
                }

            }
        })
    }


    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private fun setupTangoConfig(tango: Tango): TangoConfig {
        // Use default configuration for Tango Service, plus color camera, low latency
        // IMU integration and drift correction.
        val config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT)
        config.putBoolean(TangoConfig.KEY_BOOLEAN_COLORCAMERA, true)
        // NOTE: Low latency integration is necessary to achieve a precise alignment of
        // virtual objects with the RGB image and produce a good AR effect.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true)
        // Drift correction allows motion tracking to recover after it loses tracking.
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DRIFT_CORRECTION, true)

        return config
    }


    /**
     * Set up the callback listeners for the Tango Service and obtain other parameters required
     * after Tango connection.
     * Listen to updates from the RGB camera.
     */
    private fun startupTango() {
        // No need to add any coordinate frame pairs since we aren't using pose data from callbacks.
        val framePairs = ArrayList<TangoCoordinateFramePair>()

        tango.connectListener(framePairs, object: Tango.OnTangoUpdateListener() {
            override fun onPoseAvailable(pose: TangoPoseData?) {
                // We are not using onPoseAvailable for this app.
            }

            override fun onXyzIjAvailable(xyzIj: TangoXyzIjData?) {
                // We are not using onXyzIjAvailable for this app.
            }

            override fun onPointCloudAvailable(pointCloud: TangoPointCloudData?) {
                // We are not using onPointCloudAvailable for this app.
            }

            override fun onTangoEvent(event: TangoEvent) {
                // We are not using onTangoEvent for this app.
            }

            override fun onFrameAvailable(cameraId: Int) {
                // Check if the frame available is for the camera we want and update its frame
                // on the view.
                if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                    // Now that we are receiving onFrameAvailable callbacks, we can switch
                    // to RENDERMODE_WHEN_DIRTY to drive the render loop from this callback.
                    // This will result in a frame rate of approximately 30FPS, in synchrony with
                    // the RGB camera driver.
                    // If you need to render at a higher rate (i.e., if you want to render complex
                    // animations smoothly) you can use RENDERMODE_CONTINUOUSLY throughout the
                    // application lifecycle.
                    if (surfaceview.renderMode != ISurface.RENDERMODE_WHEN_DIRTY) {
                        surfaceview.renderMode = ISurface.RENDERMODE_WHEN_DIRTY
                    }

                    // Mark a camera frame as available for rendering in the OpenGL thread.
                    mIsFrameAvailableTangoThread.set(true);
                    // Trigger an OpenGL render to update the OpenGL scene with the new RGB data.
                    surfaceview.requestRender()
                }
            }
        })

        // Get extrinsics from device for use in transforms. This needs
        // to be done after connecting Tango and listeners.
        mExtrinsics = setupExtrinsics(tango)
        mIntrinsics = tango.getCameraIntrinsics(TangoCameraIntrinsics.TANGO_CAMERA_COLOR)
    }


    /**
     * Connects the view and renderer to the color camara and callbacks.
     */
    private fun connectRenderer() {
        // Connect to color camera.
        surfaceview.connectToTangoCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR)

        // Register a Rajawali Scene Frame Callback to update the scene camera pose whenever a new
        // RGB frame is rendered.
        // (@see https://github.com/Rajawali/Rajawali/wiki/Scene-Frame-Callbacks)
        arRenderer.getCurrentScene().registerFrameCallback(object : ASceneFrameCallback() {
            override fun onPreFrame(sceneTime: Long, deltaTime: Double) {
                if (!isConnected.get()) {
                    return
                }
                // NOTE: This is called from the OpenGL render thread, after all the renderer
                // onRender callbacks had a chance to run and before scene objects are rendered
                // into the scene.

                // Note that the TangoRajwaliRenderer will update the RGB frame to the background
                // texture and update the RGB timestamp before this callback is executed.

                // If a new RGB frame has been rendered, update the camera pose to match.
                // NOTE: This doesn't need to be synchronized since the renderer provided timestamp
                // is also set in this same OpenGL thread.
                val rgbTimestamp = arRenderer.getTimestamp()
                if (rgbTimestamp > mCameraPoseTimestamp) {
                    // Calculate the device pose at the camera frame update time.
                    val lastFramePose = tango.getPoseAtTime(rgbTimestamp, FRAME_PAIR)
                    if (lastFramePose.statusCode == TangoPoseData.POSE_VALID) {
                        // Update the camera pose from the renderer
                        arRenderer.updateRenderCameraPose(lastFramePose, mExtrinsics)
                        mCameraPoseTimestamp = lastFramePose.timestamp
                    } else {
                        Log.w(TAG, "Unable to get device pose at time: " + rgbTimestamp)
                    }
                }
            }

            override fun onPreDraw(sceneTime: Long, deltaTime: Double) {

            }

            override fun onPostFrame(sceneTime: Long, deltaTime: Double) {

            }

            override fun callPreFrame(): Boolean {
                return true
            }
        })
    }

    private fun setupRenderer() {
        surfaceview.preserveEGLContextOnPause = true
        surfaceview.setTransparent(true)
        surfaceview.setEGLContextClientVersion(2)
        surfaceview.setEGLConfigChooser(8, 8, 8, 8, 16, 0)

//        bgRenderer.createOnGlThread(this)
//        session.setCameraTextureName(bgRenderer.textureId)

        arRenderer.currentScene.registerFrameCallback(object: ASceneFrameCallback() {

            override fun onPreFrame(sceneTime: Long, deltaTime: Double) {
                // Clear screen to notify driver it should not load any pixels from previous frame.
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

                try {
                    // Obtain the current frame from ARSession. When the configuration is set to
                    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                    // camera framerate.
                    val frame = session.update()

//                    handleTaps(frame)

                    // Draw background.
//                    bgRenderer.draw(frame)



                    // If not tracking, don't draw 3d objects.
                    if (frame.trackingState == Frame.TrackingState.NOT_TRACKING) {
                        return
                    }

                    // Get projection matrix.
                    val projmtx = FloatArray(16)
                    session.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f) //10cm to 100m

                    // Get camera matrix and draw.
                    val viewmtx = FloatArray(16)
                    frame.getViewMatrix(viewmtx, 0)

                    // Compute lighting from average intensity of the image.
//                    val lightIntensity = frame.lightEstimate.pixelIntensity

                    // Visualize tracked points.
//                    pointCloud.update(frame.pointCloud)
//                    pointCloud.draw(frame.pointCloudPose, viewmtx, projmtx)

                    arRenderer.updateRenderCameraPose(frame.pose)
                    arRenderer.setProjectionMatrix(projmtx)

                    // Check if we detected at least one plane. If so, hide the loading message.
                    if (loadingMessageSnackbar != null) {
                        for (plane in session.allPlanes) {
                            if (plane.type == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                                    && plane.trackingState == Plane.TrackingState.TRACKING) {
                                hideLoadingMessage()
                                break
                            }
                        }
                    }

                    // Visualize planes.
//                    planeRenderer.drawPlanes(session.allPlanes, frame.pose, projmtx)

                    // Visualize anchors created by touch.
//                    val scaleFactor = 1.0f

                    // Get the current combined pose of an Anchor and Plane in world space. The Anchor
                    // and Plane poses are updated during calls to session.update() as ARCore refines
                    // its estimate of the world.
//                    touches.filter { it.isTracking }
//                            .forEach {
//                                it.pose.toMatrix(anchorMatrix, 0)
//
//                                // Update and draw the model and its shadow.
//                                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor)
//                                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor)
//
//                                virtualObject.draw(viewmtx, projmtx, lightIntensity)
//                                virtualObjectShadow.draw(viewmtx, projmtx, lightIntensity)
//                            }

                } catch (t: Throwable) {
                    // Avoid crashing the application due to unhandled exceptions.
                    Log.e(TAG, "Exception on the OpenGL thread", t)
                }

            }

            override fun callPreFrame(): Boolean {
                return true
            }

            override fun onPreDraw(sceneTime: Long, deltaTime: Double) {
            }

            override fun onPostFrame(sceneTime: Long, deltaTime: Double) {
            }
        })

        surfaceview.setSurfaceRenderer(arRenderer)
        surfaceview.renderMode = ISurface.RENDERMODE_CONTINUOUSLY

        surfaceview.connectToTangoCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR)

    }


    /**
     * Calculates and stores the fixed transformations between the device and
     * the various sensors to be used later for transformations between frames.
     */
    private fun setupExtrinsics(tango: Tango): DeviceExtrinsics {
        // Create camera to IMU transform.
        val framePair = TangoCoordinateFramePair()
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR
        val imuTrgbPose = tango.getPoseAtTime(0.0, framePair)

        // Create device to IMU transform.
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE
        val imuTdevicePose = tango.getPoseAtTime(0.0, framePair)

        // Create depth camera to IMU transform.
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH
        val imuTdepthPose = tango.getPoseAtTime(0.0, framePair)

        return DeviceExtrinsics(imuTdevicePose, imuTrgbPose, imuTdepthPose)
    }

    /**
     * Query the display's rotation.
     */
    private fun setDisplayRotation() {
        val display = windowManager.defaultDisplay
        displayRotation = display.rotation
    }


    private fun showLoadingMessage() {
        runOnUiThread {
            loadingMessageSnackbar = Snackbar.make(
                    this@MainActivity.findViewById(android.R.id.content),
                    "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE)
            loadingMessageSnackbar?.view?.setBackgroundColor(-0x40cdcdce)
            loadingMessageSnackbar?.show()
        }
    }

    private fun hideLoadingMessage() {
        runOnUiThread {
            loadingMessageSnackbar?.dismiss()
            loadingMessageSnackbar = null
        }
    }

    /**
     * Display toast on UI thread.
     *
     * @param resId The resource id of the string resource to use. Can be formatted text.
     */
    private fun showsToastAndFinishOnUiThread(resId: Int) {
        runOnUiThread {
            Toast.makeText(this@MainActivity,
                    getString(resId), Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
