package com.raywenderlich.android.myexpedition.rendering

import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import com.google.ar.core.Pose
import com.google.atap.tangoservice.TangoPoseData
import com.google.atap.tangoservice.TangoXyzIjData
import com.raywenderlich.android.myexpedition.renderables.FrustumAxes
import com.raywenderlich.android.myexpedition.renderables.Grid
import com.raywenderlich.android.myexpedition.renderables.PointCloud
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.Matrix4
import com.google.atap.tangoservice.TangoPointCloudData
import org.rajawali3d.math.vector.Vector3


/**
 * Renderer for Point Cloud data.
 */
class PointCloudRajawaliRenderer(context: Context) : Renderer(context) {

    private val mTouchViewHandler: TouchViewHandler = TouchViewHandler(mContext, currentCamera)
    private val mDeviceExtrinsics: DeviceExtrinsics? = null

    // Objects rendered in the scene
    private var mPointCloud: PointCloud? = null
    private var mFrustumAxes: FrustumAxes? = null
    private var mGrid: Grid? = null

    override fun initScene() {
        mGrid = Grid(100, 1, 1F, -0x333334)
        mGrid!!.setPosition(0.0, -1.3, 0.0)
        currentScene.addChild(mGrid)

        mFrustumAxes = FrustumAxes(3F)
        currentScene.addChild(mFrustumAxes)

        mPointCloud = PointCloud(MAX_NUMBER_OF_POINTS)
        currentScene.addChild(mPointCloud)
        currentScene.backgroundColor = Color.WHITE
        currentCamera.nearPlane = CAMERA_NEAR
        currentCamera.farPlane = CAMERA_FAR
        currentCamera.fieldOfView = 37.5
    }

    /**
     * Updates the rendered point cloud. For this, we need the point cloud data and the device pose
     * at the time the cloud data was acquired.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updatePointCloud(pointCloudData: TangoPointCloudData, openGlTdepth: FloatArray) {
        mPointCloud?.updateCloud(pointCloudData.numPoints, pointCloudData.points)
        val openGlTdepthMatrix = Matrix4(openGlTdepth)
        mPointCloud?.setPosition(openGlTdepthMatrix.translation)
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention.
        mPointCloud?.setOrientation(Quaternion().fromMatrix(openGlTdepthMatrix).conjugate())
    }

    /**
     * Updates our information about the current device pose.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updateCameraPose(cameraPose: TangoPoseData) {
        val rotation = cameraPose.rotationAsFloats
        val translation = cameraPose.translationAsFloats
        val quaternion = Quaternion(rotation[3].toDouble(), rotation[0].toDouble(), rotation[1].toDouble(), rotation[2].toDouble())
        mFrustumAxes?.setPosition(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble())
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for
        // quaternions.
        mFrustumAxes?.orientation = quaternion.conjugate()
        mTouchViewHandler.updateCamera(Vector3(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble()),
                quaternion)
    }


    /**
     * Updates the rendered point cloud. For this, we need the point cloud data and the device pose
     * at the time the cloud data was acquired.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updatePointCloud(pointCloudData: com.google.ar.core.PointCloud, openGlTdepth: FloatArray) {
        mPointCloud?.updateCloud(pointCloudData.points.capacity(), pointCloudData.points)
        val openGlTdepthMatrix = Matrix4(openGlTdepth)
        mPointCloud?.position = openGlTdepthMatrix.translation
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention.
        mPointCloud?.orientation = Quaternion().fromMatrix(openGlTdepthMatrix).conjugate()
    }


    /**
     * Updates our information about the current device pose.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updateCameraPose(cameraPose: Pose) {
        val rotation = cameraPose.extractRotation()
        val translation = cameraPose.extractTranslation()
        val quaternion = Quaternion(rotation.qw().toDouble(), rotation.qx().toDouble(), rotation.qy().toDouble(), rotation.qz().toDouble() )
        mFrustumAxes?.setPosition(translation.qx().toDouble(), translation.qy().toDouble(), translation.qz().toDouble())

        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for quaternions.
        mFrustumAxes?.orientation = quaternion.conjugate()

        mTouchViewHandler.updateCamera(Vector3(translation.qx().toDouble(), translation.qy().toDouble(), translation.qz().toDouble()), quaternion)
    }


    /**
     * Updates the rendered point cloud. For this, we need the point cloud data and the device pose
     * at the time the cloud data was acquired.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updatePointCloud(xyzIjData: TangoXyzIjData, devicePose: TangoPoseData, extrinsics: DeviceExtrinsics) {
        val pointCloudPose = ScenePoseCalculator.toDepthCameraOpenGlPose(devicePose, extrinsics)
        mPointCloud?.updateCloud(xyzIjData.xyzCount, xyzIjData.xyz)
        mPointCloud?.position = pointCloudPose.position
        mPointCloud?.orientation = pointCloudPose.orientation
    }

    /**
     * Updates our information about the current device pose.
     * NOTE: This needs to be called from the OpenGL rendering thread.
     */
    fun updateDevicePose(tangoPoseData: TangoPoseData, extrinsics: DeviceExtrinsics) {
        val cameraPose = ScenePoseCalculator.toOpenGlCameraPose(tangoPoseData, extrinsics)
        mFrustumAxes?.position = cameraPose.position
        mFrustumAxes?.orientation = cameraPose.orientation
        mTouchViewHandler.updateCamera(cameraPose.position, cameraPose.orientation)
    }

    override fun onOffsetsChanged(v: Float, v1: Float, v2: Float, v3: Float, i: Int, i1: Int) {}

    override fun onTouchEvent(motionEvent: MotionEvent) {
        mTouchViewHandler.onTouchEvent(motionEvent)
    }

    fun setFirstPersonView() {
        mTouchViewHandler.setFirstPersonView()
    }

    fun setTopDownView() {
        mTouchViewHandler.setTopDownView()
    }

    fun setThirdPersonView() {
        mTouchViewHandler.setThirdPersonView()
    }

    companion object {

        private val CAMERA_NEAR = 0.01
        private val CAMERA_FAR = 200.0
        private val MAX_NUMBER_OF_POINTS = 60000
    }
}