package com.raywenderlich.android.myexpedition.rendering

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import com.google.ar.core.Pose
import com.google.atap.tangoservice.TangoPoseData
import com.raywenderlich.android.myexpedition.R
import org.rajawali3d.Object3D
import org.rajawali3d.animation.Animation
import org.rajawali3d.animation.EllipticalOrbitAnimation3D
import org.rajawali3d.animation.RotateOnAxisAnimation
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.methods.DiffuseMethod
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.materials.textures.StreamingTexture
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.math.Matrix4
import org.rajawali3d.math.Quaternion
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.primitives.Sphere
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.util.ObjectColorPicker
import org.rajawali3d.util.OnObjectPickedListener
import javax.microedition.khronos.opengles.GL10


/**
 * Renderer that implements a basic augmented reality scene using Rajawali.
 * It creates a scene with a background quad taking the whole screen, where the color camera is
 * rendered and a sphere with the texture of the earth floats ahead of the start position of
 * the Tango device.
 */
class AugmentedRealityRenderer(context: Context) : RajawaliRenderer(context), OnObjectPickedListener {


    private val textureCoords0 = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    // Rajawali texture used to render the Tango color camera.
    private var mTangoCameraTexture: ATexture? = null

    // Keeps track of whether the scene camera has been configured.
    var isSceneCameraConfigured: Boolean = false
        private set

    private var mBackgroundQuad: ScreenQuad? = null

    private var mOnePicker: ObjectColorPicker? = null

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread; it is not thread-safe.
     */
    val textureId: Int
        get() = if (mTangoCameraTexture == null) -1 else mTangoCameraTexture!!.textureId

    override fun initScene() {
        mOnePicker = ObjectColorPicker(this)
        mOnePicker!!.setOnObjectPickedListener(this)

        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        val tangoCameraMaterial = Material()
        tangoCameraMaterial.colorInfluence = 0f

        if (mBackgroundQuad == null) {
            mBackgroundQuad = ScreenQuad()
            mBackgroundQuad!!.geometry.setTextureCoords(textureCoords0)
        }
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering.
        mTangoCameraTexture = StreamingTexture("camera", null as StreamingTexture.ISurfaceListener?)
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture)
            mBackgroundQuad!!.material = tangoCameraMaterial
        } catch (e: ATexture.TextureException) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e)
        }

        currentScene.addChildAt(mBackgroundQuad, 0)

        // Add a directional light in an arbitrary direction.
        val light = DirectionalLight(1.0, 0.2, -1.0)
        light.setColor(1f, 1f, 1f)
        light.power = 0.8f
        light.setPosition(3.0, 2.0, 4.0)
        currentScene.addLight(light)

        // Create sphere with earth texture and place it in space 3m forward from the origin.
        val earthMaterial = Material()
        try {
            val t = Texture("earth", R.mipmap.earth)
            earthMaterial.addTexture(t)
        } catch (e: ATexture.TextureException) {
            Log.e(TAG, "Exception generating earth texture", e)
        }

        earthMaterial.colorInfluence = 0f
        earthMaterial.enableLighting(true)
        earthMaterial.diffuseMethod = DiffuseMethod.Lambert()
        val earth = Sphere(0.4f, 20, 20)
        earth.material = earthMaterial
        earth.setPosition(0.0, 0.0, -3.0)
        currentScene.addChild(earth)

        // Rotate around its Y axis
        val animEarth = RotateOnAxisAnimation(Vector3.Axis.Y, 0.0, -360.0)
        animEarth.interpolator = LinearInterpolator()
        animEarth.durationMilliseconds = 60000
        animEarth.repeatMode = Animation.RepeatMode.INFINITE
        animEarth.transformable3D = earth
        currentScene.registerAnimation(animEarth)
        animEarth.play()

        // Create sphere with moon texture.
        val moonMaterial = Material()
        try {
            val t = Texture("moon", R.mipmap.moon)
            moonMaterial.addTexture(t)
        } catch (e: ATexture.TextureException) {
            Log.e(TAG, "Exception generating moon texture", e)
        }

        moonMaterial.colorInfluence = 0f
        moonMaterial.enableLighting(true)
        moonMaterial.diffuseMethod = DiffuseMethod.Lambert()
        val moon = Sphere(0.1f, 20, 20)
        moon.material = moonMaterial
        moon.setPosition(0.0, 0.0, -1.0)
        currentScene.addChild(moon)

        // Rotate the moon around its Y axis.
        val animMoon = RotateOnAxisAnimation(Vector3.Axis.Y, 0.0, -360.0)
        animMoon.interpolator = LinearInterpolator()
        animMoon.durationMilliseconds = 60000
        animMoon.repeatMode = Animation.RepeatMode.INFINITE
        animMoon.transformable3D = moon
        currentScene.registerAnimation(animMoon)
        animMoon.play()

        // Make the moon orbit around the earth. The first two parameters are the focal point and
        // periapsis of the orbit.
        val translationMoon = EllipticalOrbitAnimation3D(Vector3(0.0, 0.0, -5.0),
                Vector3(0.0, 0.0, -1.0), Vector3.getAxisVector(Vector3.Axis.Y), 0.0,
                360.0, EllipticalOrbitAnimation3D.OrbitDirection.COUNTERCLOCKWISE)
        translationMoon.durationMilliseconds = 60000
        translationMoon.repeatMode = Animation.RepeatMode.INFINITE
        translationMoon.transformable3D = moon
        currentScene.registerAnimation(translationMoon)
        translationMoon.play()

        mOnePicker!!.registerObject(earth)
        mOnePicker!!.registerObject(moon)
        mOnePicker!!.registerObject(mBackgroundQuad)
    }

    /**
     * Update background texture's UV coordinates when device orientation is changed (i.e., change
     * between landscape and portrait mode).
     * This must be run in the OpenGL thread.
     */
    fun updateColorCameraTextureUvGlThread(rotation: Int) {
        if (mBackgroundQuad == null) {
            mBackgroundQuad = ScreenQuad()
        }

//        val textureCoords = TangoSupport.getVideoOverlayUVBasedOnDisplayRotation(textureCoords0, rotation)
//        mBackgroundQuad!!.geometry.setTextureCoords(textureCoords, true)
        mBackgroundQuad!!.geometry.reload()
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The device pose should match the pose of the device at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     *
     *
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    fun updateRenderCameraPose(devicePose: TangoPoseData, extrinsics: DeviceExtrinsics) {
        val cameraPose = ScenePoseCalculator.toOpenGlCameraPose(devicePose, extrinsics)
        currentCamera.setRotation(cameraPose.orientation)
        currentCamera.position = cameraPose.position
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time of the last rendered
     * RGB frame, which can be retrieved with this.getTimestamp();
     *
     *
     * NOTE: This must be called from the OpenGL render thread; it is not thread-safe.
     */
    fun updateRenderCameraPose(cameraPose: TangoPoseData) {
        val rotation = cameraPose.rotationAsFloats
        val translation = cameraPose.translationAsFloats
        val quaternion = Quaternion(rotation[3].toDouble(), rotation[0].toDouble(), rotation[1].toDouble(), rotation[2].toDouble())
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for
        // quaternions.
        currentCamera.setRotation(quaternion.conjugate())
        currentCamera.setPosition(translation[0].toDouble(), translation[1].toDouble(), translation[2].toDouble())
    }

    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time of the last rendered
     * RGB frame, which can be retrieved with this.getTimestamp();
     *
     *
     * NOTE: This must be called from the OpenGL render thread; it is not thread-safe.
     */
    fun updateRenderCameraPose(cameraPose: Pose) {
        val rotation = cameraPose.extractRotation()
        val translation = cameraPose.extractTranslation()
        val quaternion = Quaternion(rotation.qw().toDouble(), rotation.qx().toDouble(), rotation.qy().toDouble(), rotation.qz().toDouble())
        // Conjugating the Quaternion is needed because Rajawali uses left-handed convention for
        // quaternions.
        currentCamera.setRotation(quaternion.conjugate())
        currentCamera.setPosition(translation.qx().toDouble(), translation.qy().toDouble(), translation.qz().toDouble())
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    override fun onRenderSurfaceSizeChanged(gl: GL10, width: Int, height: Int) {
        super.onRenderSurfaceSizeChanged(gl, width, height)
        isSceneCameraConfigured = false
    }

    override fun onTouchEvent(event: MotionEvent?) {
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the `TangoCameraIntrinsics`.
     */
    fun setProjectionMatrix(matrixFloats: FloatArray) {
        currentCamera.projectionMatrix = Matrix4(matrixFloats)
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float,
                                  xOffsetStep: Float, yOffsetStep: Float,
                                  xPixelOffset: Int, yPixelOffset: Int) {
    }

    override fun onObjectPicked(`object`: Object3D) {
        Log.d(TAG, "Picked object: " + `object`)
    }

    override fun onNoObjectPicked() {
        Log.d(TAG, "Picked no object")
    }

    companion object {
        private val TAG = AugmentedRealityRenderer::class.java.simpleName
    }
}
