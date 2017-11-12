package com.raywenderlich.android.myexpedition.rendering

import android.content.Context
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES20
import android.util.Log
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import com.google.ar.core.Session
import com.raywenderlich.android.myexpedition.R
import org.rajawali3d.animation.Animation
import org.rajawali3d.animation.EllipticalOrbitAnimation3D
import org.rajawali3d.animation.RotateOnAxisAnimation
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.methods.DiffuseMethod
import org.rajawali3d.materials.shaders.FragmentShader
import org.rajawali3d.materials.shaders.VertexShader
import org.rajawali3d.materials.textures.ATexture
import org.rajawali3d.materials.textures.StreamingTexture
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.primitives.ScreenQuad
import org.rajawali3d.primitives.Sphere
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.util.ObjectColorPicker
import org.rajawali3d.util.RawShaderLoader
import javax.microedition.khronos.opengles.GL10


/**
 * Renderer that implements a basic augmented reality scene using Rajawali.
 * It creates a scene with a background quad taking the whole screen, where the color camera is
 * rendered and a sphere with the texture of the earth floats ahead of the start position of
 * the device.
 */

class AugmentedRealityRajawaliRenderer(context: Context, private val session: Session, private val mediaPlayer: MediaPlayer) : Renderer(context) {

    private lateinit var objColorPicker: ObjectColorPicker
    private lateinit var screenBackground: ScreenQuad

    private val textureCoords0 = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)

    val vertexShader = VertexShader(R.raw.screenquad_vertex)
    val fragmentShader = FragmentShader(R.raw.screenquad_fragment_oes)

//    private lateinit var cameraTexture: StreamingTexture
    private lateinit var videoTexture: StreamingTexture


    override fun initScene() {
        // -- set the background color to be transparent
        // you need to have called setTransparent(true); in the activity
        // for this to work.
        currentScene.backgroundColor = Color.TRANSPARENT
        currentCamera.z = 4.2


        objColorPicker = ObjectColorPicker(this)

        // Create a quad covering the whole background and assign a texture to it where the
        // color camera contents will be rendered.
        screenBackground = ScreenQuad(true)
        screenBackground.geometry.setTextureCoords(textureCoords0)

        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering.
//        cameraTexture = StreamingTexture("camera", null as StreamingTexture.ISurfaceListener?)
        videoTexture = StreamingTexture("video", mediaPlayer)

        try {

//            val cameraMaterial = Material(vertexShader, fragmentShader)
            val cameraMaterial = Material()
            cameraMaterial.colorInfluence = 0.0F

//            session.setCameraTextureName(cameraTexture.textureId)
//            cameraMaterial.addTexture(cameraTexture)
            cameraMaterial.addTexture(videoTexture)
            screenBackground.material = cameraMaterial
        } catch (e: ATexture.TextureException) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e)
        }

        currentScene.addChildAt(screenBackground, 0)


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


        objColorPicker.registerObject(earth)
        objColorPicker.registerObject(moon)
        objColorPicker.registerObject(screenBackground)

    }


    override fun onRenderFrame(gl: GL10?) {
        super.onRenderFrame(gl)

//        cameraTexture.update()
        videoTexture.update()
    }

    override fun onRenderSurfaceDestroyed(surface: SurfaceTexture?) {
        super.onRenderSurfaceDestroyed(surface)

        mediaPlayer.stop()
        mediaPlayer.release()
    }


    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {
    }

    override fun onTouchEvent(event: MotionEvent?) {
    }

    companion object {
        private val TAG = AugmentedRealityRajawaliRenderer::class.java.simpleName
    }

}
