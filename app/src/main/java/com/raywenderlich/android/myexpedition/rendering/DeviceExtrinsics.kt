package com.raywenderlich.android.myexpedition.rendering

import com.google.atap.tangoservice.TangoPoseData
import org.rajawali3d.math.Matrix4


/**
 * Class used to hold device extrinsics information in a way that is easy to use to perform
 * transformations with the ScenePoseCalculator.
 */
class DeviceExtrinsics(imuTDevicePose: TangoPoseData, imuTColorCameraPose: TangoPoseData,
                       imuTDepthCameraPose: TangoPoseData) {
    // Transformation from the position of the depth camera to the device frame.
    val deviceTDepthCamera: Matrix4

    // Transformation from the position of the color Camera to the device frame.
    val deviceTColorCamera: Matrix4

    init {
        val deviceTImu = ScenePoseCalculator.tangoPoseToMatrix(imuTDevicePose).inverse()
        val imuTColorCamera = ScenePoseCalculator.tangoPoseToMatrix(imuTColorCameraPose)
        val imuTDepthCamera = ScenePoseCalculator.tangoPoseToMatrix(imuTDepthCameraPose)
        deviceTDepthCamera = deviceTImu.clone().multiply(imuTDepthCamera)
        deviceTColorCamera = deviceTImu.multiply(imuTColorCamera)
    }
}
