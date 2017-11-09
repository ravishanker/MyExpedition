package com.raywenderlich.android.myexpedition.rendering

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import org.rajawali3d.cameras.Camera;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;


/**
 * This is a helper class that adds top-down and third-person views in a VR setting, including
 * handling of standard pan and zoom touch interactions.
 */
class TouchViewHandler(context: Context, var camera: Camera?) {

    private var viewMode = ViewMode.THIRD_PERSON

    private val gestureDetector: GestureDetector
    private val scaleGestureDetector: ScaleGestureDetector

    private var topDownCameraDelta = Vector3()
    private var thirdPersonPitch = TOUCH_THIRD_PITCH_DEFAULT
    private var thirdPersonYaw = TOUCH_THIRD_YAW_DEFAULT

    private enum class ViewMode {
        FIRST_PERSON, TOP_DOWN, THIRD_PERSON
    }

    init {
        gestureDetector = GestureDetector(context, DragListener())
        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    fun updateCamera(position: Vector3, orientation: Quaternion) {
        if (viewMode == ViewMode.FIRST_PERSON) {
            camera!!.position = position
            camera!!.orientation = orientation
        } else if (viewMode == ViewMode.TOP_DOWN) {
            camera!!.setPosition(position.x + topDownCameraDelta.x, TOUCH_TOP_DISTANCE,
                    position.z + topDownCameraDelta.z)
            camera!!.setRotation(Vector3.Axis.X, 90.0)
        } else if (viewMode == ViewMode.THIRD_PERSON) {
            camera!!.setPosition(position.x, position.y, position.z)
            camera!!.setRotZ(thirdPersonPitch)
            camera!!.rotate(Vector3.Axis.Y, thirdPersonYaw)
            camera!!.moveForward(TOUCH_THIRD_DISTANCE)
        }
    }

    fun onTouchEvent(motionEvent: MotionEvent) {
        gestureDetector.onTouchEvent(motionEvent)
        scaleGestureDetector.onTouchEvent(motionEvent)
    }

    fun setFirstPersonView() {
        viewMode = ViewMode.FIRST_PERSON
        camera!!.fieldOfView = FIRST_PERSON_FOV
    }

    fun setTopDownView() {
        viewMode = ViewMode.TOP_DOWN
        topDownCameraDelta = Vector3()
        camera!!.setFieldOfView(TOP_DOWN_FOV)
    }

    fun setThirdPersonView() {
        viewMode = ViewMode.THIRD_PERSON
        thirdPersonYaw = TOUCH_THIRD_YAW_DEFAULT
        thirdPersonPitch = TOUCH_THIRD_PITCH_DEFAULT
        camera!!.fieldOfView = THIRD_PERSON_FOV
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        internal var scale = 1f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale = detector.scaleFactor
            scale = Math.max(0.1f, Math.min(scale, 5f))

            camera!!.fieldOfView = Math.min(camera!!.fieldOfView / scale, TOUCH_FOV_MAX)

            return true
        }
    }

    private inner class DragListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {

            if (viewMode == ViewMode.TOP_DOWN) {
                val factor = camera!!.getFieldOfView() / 45
                topDownCameraDelta.add(
                        Vector3(distanceX / 100 * factor, 0.0, distanceY / 100 * factor))
            } else if (viewMode == ViewMode.THIRD_PERSON) {
                thirdPersonPitch -= distanceY / 10
                thirdPersonPitch = Math.min(thirdPersonPitch, TOUCH_THIRD_PITCH_LIMIT)
                thirdPersonPitch = Math.max(thirdPersonPitch, (-TOUCH_THIRD_PITCH_LIMIT))
                thirdPersonYaw -= distanceX / 10
                thirdPersonYaw %= 360f
            }

            return true
        }
    }

    companion object {
        // Touch interaction tuning constants.
        private val TOUCH_THIRD_PITCH_LIMIT = 60.0
        private val TOUCH_THIRD_PITCH_DEFAULT = 45.0
        private val TOUCH_THIRD_YAW_DEFAULT = -45.0
        private val TOUCH_FOV_MAX = 120.0
        private val TOUCH_THIRD_DISTANCE = 10.0
        private val TOUCH_TOP_DISTANCE = 10.0

        // Virtual reality view parameters.
        private val FIRST_PERSON_FOV = 37.8
        private val THIRD_PERSON_FOV = 65.0
        private val TOP_DOWN_FOV = 65.0
    }
}
