package com.auramusic.app.tv

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sin

/** Renders source art and the animated waveform directly into a MediaCodec input surface. */
internal class TvPreviewEglRenderer(
    private val surface: Surface,
    artwork: Bitmap,
) : AutoCloseable {
    private val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val config: android.opengl.EGLConfig
    private val context: android.opengl.EGLContext
    private val eglSurface: android.opengl.EGLSurface
    private val textureProgram: Int
    private val colorProgram: Int
    private val textureId: Int
    private val texturePosition: Int
    private val textureCoordinate: Int
    private val textureSampler: Int
    private val colorPosition: Int
    private val colorUniform: Int
    private val quadPositions: FloatBuffer = floatBuffer(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val quadCoordinates: FloatBuffer
    private val panelCoordinates = floatBuffer(FloatArray(8))
    private val waveformCoordinates = floatBuffer(FloatArray((WAVE_POINTS + 1) * 2))
    // glDrawArrays count is vertices; the backing buffer stores two floats per vertex.
    private val waveformVertices = WAVE_POINTS + 1

    init {
        check(display != EGL14.EGL_NO_DISPLAY) { "Could not get EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) { "Could not initialize EGL" }
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val configCount = IntArray(1)
        val attributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_NONE,
        )
        check(EGL14.eglChooseConfig(display, attributes, 0, configs, 0, configs.size, configCount, 0)) {
            "Could not select EGL config"
        }
        config = requireNotNull(configs[0]) { "EGL returned no config" }
        context = EGL14.eglCreateContext(
            display,
            config,
            EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
            0,
        )
        check(context != EGL14.EGL_NO_CONTEXT) { "Could not create EGL context" }
        eglSurface = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "Could not create EGL window surface" }
        makeCurrent()

        textureProgram = linkProgram(TEXTURE_VERTEX_SHADER, TEXTURE_FRAGMENT_SHADER)
        colorProgram = linkProgram(COLOR_VERTEX_SHADER, COLOR_FRAGMENT_SHADER)
        texturePosition = GLES20.glGetAttribLocation(textureProgram, "aPosition")
        textureCoordinate = GLES20.glGetAttribLocation(textureProgram, "aTexCoord")
        textureSampler = GLES20.glGetUniformLocation(textureProgram, "uTexture")
        colorPosition = GLES20.glGetAttribLocation(colorProgram, "aPosition")
        colorUniform = GLES20.glGetUniformLocation(colorProgram, "uColor")

        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, artwork, 0)
        quadCoordinates = cropCoordinates(artwork.width, artwork.height)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun drawFrame(frameIndex: Int, presentationTimeUs: Long) {
        makeCurrent()
        EGLExt.eglPresentationTimeANDROID(display, eglSurface, presentationTimeUs * 1_000L)
        GLES20.glViewport(0, 0, WIDTH, HEIGHT)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(textureProgram)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(textureSampler, 0)
        GLES20.glEnableVertexAttribArray(texturePosition)
        GLES20.glVertexAttribPointer(texturePosition, 2, GLES20.GL_FLOAT, false, 0, quadPositions.position(0))
        GLES20.glEnableVertexAttribArray(textureCoordinate)
        GLES20.glVertexAttribPointer(textureCoordinate, 2, GLES20.GL_FLOAT, false, 0, quadCoordinates.position(0))
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        drawSolid(panelVertices(), PANEL_RED, PANEL_GREEN, PANEL_BLUE, PANEL_ALPHA)
        val wave = FloatArray((WAVE_POINTS + 1) * 2)
        for (point in 0..WAVE_POINTS) {
            val x = PANEL_LEFT + 20f + point * ((PANEL_RIGHT - PANEL_LEFT - 40f) / WAVE_POINTS)
            val envelope = 0.26f + 0.74f * sin(point * Math.PI / WAVE_POINTS).toFloat()
            val phase = frameIndex * (Math.PI / 2.0) + point * 0.72
            val y = PANEL_CENTER_Y + 24f * envelope * sin(phase).toFloat()
            wave[point * 2] = pixelToClipX(x)
            wave[point * 2 + 1] = pixelToClipY(y)
        }
        waveformCoordinates.position(0)
        waveformCoordinates.put(wave).position(0)
        GLES20.glUseProgram(colorProgram)
        GLES20.glUniform4f(colorUniform, WAVE_RED, WAVE_GREEN, WAVE_BLUE, 1f)
        GLES20.glEnableVertexAttribArray(colorPosition)
        GLES20.glVertexAttribPointer(colorPosition, 2, GLES20.GL_FLOAT, false, 0, waveformCoordinates)
        GLES20.glLineWidth(8f)
        GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, waveformVertices)
        check(EGL14.eglSwapBuffers(display, eglSurface)) { "Could not submit encoded frame" }
    }

    private fun drawSolid(vertices: FloatArray, red: Float, green: Float, blue: Float, alpha: Float) {
        panelCoordinates.position(0)
        panelCoordinates.put(vertices).position(0)
        GLES20.glUseProgram(colorProgram)
        GLES20.glUniform4f(colorUniform, red, green, blue, alpha)
        GLES20.glEnableVertexAttribArray(colorPosition)
        GLES20.glVertexAttribPointer(colorPosition, 2, GLES20.GL_FLOAT, false, 0, panelCoordinates)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun panelVertices() = floatArrayOf(
        pixelToClipX(PANEL_LEFT), pixelToClipY(PANEL_TOP),
        pixelToClipX(PANEL_LEFT), pixelToClipY(PANEL_BOTTOM),
        pixelToClipX(PANEL_RIGHT), pixelToClipY(PANEL_TOP),
        pixelToClipX(PANEL_RIGHT), pixelToClipY(PANEL_BOTTOM),
    )

    private fun cropCoordinates(sourceWidth: Int, sourceHeight: Int): FloatBuffer {
        val targetAspect = WIDTH.toFloat() / HEIGHT
        val sourceAspect = sourceWidth.toFloat() / sourceHeight
        val left: Float
        val right: Float
        val top: Float
        val bottom: Float
        if (sourceAspect > targetAspect) {
            val visible = targetAspect / sourceAspect
            left = (1f - visible) / 2f
            right = 1f - left
            top = 0f
            bottom = 1f
        } else {
            val visible = sourceAspect / targetAspect
            top = (1f - visible) / 2f
            bottom = 1f - top
            left = 0f
            right = 1f
        }
        // Bitmap row zero is the visual top; GL's v=0 samples that row.
        return floatBuffer(floatArrayOf(left, bottom, right, bottom, left, top, right, top))
    }

    private fun makeCurrent() {
        check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) { "Could not bind EGL context" }
    }

    override fun close() {
        runCatching { makeCurrent() }
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        GLES20.glDeleteProgram(textureProgram)
        GLES20.glDeleteProgram(colorProgram)
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, eglSurface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
        surface.release()
    }

    private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertex)
        GLES20.glAttachShader(program, fragment)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "Could not link preview GL program: ${GLES20.glGetProgramInfoLog(program)}" }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { "Could not compile preview GL shader: ${GLES20.glGetShaderInfoLog(shader)}" }
        return shader
    }

    private fun pixelToClipX(x: Float) = x * 2f / WIDTH - 1f
    private fun pixelToClipY(y: Float) = 1f - y * 2f / HEIGHT

    private fun floatBuffer(values: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values).position(0) }

    private companion object {
        const val WIDTH = 640
        const val HEIGHT = 360
        const val WAVE_POINTS = 48
        const val PANEL_LEFT = 204f
        const val PANEL_RIGHT = 436f
        const val PANEL_TOP = 252f
        const val PANEL_BOTTOM = 338f
        const val PANEL_CENTER_Y = (PANEL_TOP + PANEL_BOTTOM) / 2f
        const val PANEL_RED = 8f / 255f
        const val PANEL_GREEN = 10f / 255f
        const val PANEL_BLUE = 20f / 255f
        const val PANEL_ALPHA = 188f / 255f
        const val WAVE_RED = 1f
        const val WAVE_GREEN = 74f / 255f
        const val WAVE_BLUE = 166f / 255f
        const val TEXTURE_VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() { gl_Position = aPosition; vTexCoord = aTexCoord; }
        """
        const val TEXTURE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTexCoord;
            void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }
        """
        const val COLOR_VERTEX_SHADER = """
            attribute vec4 aPosition;
            void main() { gl_Position = aPosition; }
        """
        const val COLOR_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            void main() { gl_FragColor = uColor; }
        """
    }
}
