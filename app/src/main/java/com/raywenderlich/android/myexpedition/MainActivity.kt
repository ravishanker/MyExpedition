package com.raywenderlich.android.myexpedition

import android.graphics.Color
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
import com.raywenderlich.android.myexpedition.rendering.AugmentedRealityRajawaliRenderer
import kotlinx.android.synthetic.main.activity_main.*
import org.rajawali3d.scene.ASceneFrameCallback
import org.rajawali3d.view.ISurface
import android.media.MediaPlayer




class MainActivity : AppCompatActivity() {

    private lateinit var session: Session
    private lateinit var defaultConfig: Config
    private lateinit var loadingMessageSnackbar: Snackbar

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var bgRenderer: AugmentedRealityRajawaliRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, R.raw.sintel_trailer_480p)
        mediaPlayer.isLooping = true

        // Step 1
        startArCoreSession()

        // Step 2
        setupSurfaceRenderer()
    }

    override fun onResume() {
        super.onResume()

        if (CameraPermissionHelper.hasCameraPermission(this)) {
            showLoadingMessage()

            // Note that order matters - see the note in onPause(), the reverse applies here.
            mediaPlayer.start()

            session.resume(defaultConfig)
            surfaceview.onResume()
        } else {
            CameraPermissionHelper.requestCameraPermission(this)
        }
    }

    override fun onPause() {
        super.onPause()

        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call mSession.update() and get a SessionPausedException.
        mediaPlayer.pause()

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

    private fun startArCoreSession() {
        session = Session(this)

        // Create default config, check is supported, create session from that config.
        defaultConfig = Config.createDefaultConfig()
        if (!session.isSupported(defaultConfig)) {
            Toast.makeText(this, "This device does not support AR", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Toast.makeText(this, "Yay! This device does support AR", Toast.LENGTH_LONG).show()
    }

    private fun setupSurfaceRenderer() {
        // Rendering. The Renderers are created here, and initialized when the GL surface is created.
        surfaceview.preserveEGLContextOnPause = true
        surfaceview.setEGLContextClientVersion(2)

        //clear background and
        surfaceview.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.

        // set it to transparent
        surfaceview.setBackgroundColor(Color.TRANSPARENT)
        surfaceview.setTransparent(true)
        surfaceview.renderMode = ISurface.RENDERMODE_CONTINUOUSLY

        // set renderer
        bgRenderer = AugmentedRealityRajawaliRenderer(this, session, mediaPlayer)
//        bgRenderer.currentScene.registerFrameCallback(SceneFrameCallback())
        surfaceview.setSurfaceRenderer(bgRenderer)
    }

    inner class SceneFrameCallback : ASceneFrameCallback() {

        override fun onPreFrame(sceneTime: Long, deltaTime: Double) {
        }

        override fun onPreDraw(sceneTime: Long, deltaTime: Double) {
        }

        override fun onPostFrame(sceneTime: Long, deltaTime: Double) {
            try {
                // Obtain the current frame from ARSession. When the configuration is set to
                // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                // camera framerate.
                val frame = session.update()

//                handleTaps(frame)

                // Draw background.
//                backgroundRenderer.draw(frame)

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
                val lightIntensity = frame.lightEstimate.pixelIntensity

                // Check if we detected at least one plane. If so, hide the loading message.
                if (loadingMessageSnackbar.isShown) {
                    for (plane in session.allPlanes) {
                        if (plane.type == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                                && plane.trackingState == Plane.TrackingState.TRACKING) {
                            hideLoadingMessage()
                            break
                        }
                    }
                }


            } catch (t: Throwable) {
                // Avoid crashing the application due to unhandled exceptions.
                Log.e(TAG, "Exception on the OpenGL thread", t)
            }

        }

        override fun callPreFrame(): Boolean {
            return false
        }

        override fun callPreDraw(): Boolean {
            return false
        }

        override fun callPostFrame(): Boolean {
            return true
        }
    }

    private fun showLoadingMessage() {
        runOnUiThread {
            loadingMessageSnackbar = Snackbar.make(
                    this@MainActivity.findViewById(android.R.id.content),
                    "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE)
            loadingMessageSnackbar.view.setBackgroundColor(-0x40cdcdce)
            loadingMessageSnackbar.show()
        }
    }

    private fun hideLoadingMessage() {
        runOnUiThread {
            loadingMessageSnackbar.dismiss()
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}
