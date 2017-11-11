package com.raywenderlich.android.myexpedition

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Session

class MainActivity : AppCompatActivity() {

    private lateinit var session: Session
    private lateinit var defaultConfig: Config
    private lateinit var loadingMessageSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Step 1
        startSession()
    }

    override fun onResume() {
        super.onResume()

        if (CameraPermissionHelper.hasCameraPermission(this)) {
            showLoadingMessage()

            session.resume(defaultConfig)
        } else {
            CameraPermissionHelper.requestCameraPermission(this)
        }
    }

    override fun onPause() {
        super.onPause()

        session.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this,
                    "Camera permission is needed to run this application", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startSession() {
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
}
