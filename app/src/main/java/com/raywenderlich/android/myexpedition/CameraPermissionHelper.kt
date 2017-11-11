package com.raywenderlich.android.myexpedition

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Helper to ask camera permission.
 */
object CameraPermissionHelper {
    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val CAMERA_PERMISSION_CODE = 0

    /**
     * Check to see we have the necessary permissions for this app.
     */
    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check to see we have the necessary permissions for this app, and ask for them if we don't.
     */
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(activity, arrayOf(CAMERA_PERMISSION),
                CAMERA_PERMISSION_CODE)
    }
}
