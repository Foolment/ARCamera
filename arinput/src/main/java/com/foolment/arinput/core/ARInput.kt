package com.foolment.arinput.core

import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_FRAGMENT_SHADER
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import android.opengl.GLES20.GL_VERTEX_SHADER
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glAttachShader
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glCreateProgram
import android.opengl.GLES20.glCreateShader
import android.opengl.GLES20.glDeleteProgram
import android.opengl.GLES20.glDeleteShader
import android.opengl.GLES20.glDisableVertexAttribArray
import android.opengl.GLES20.glDrawArrays
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glGetAttribLocation
import android.opengl.GLES20.glGetUniformLocation
import android.opengl.GLES20.glLinkProgram
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES20.glUniform1i
import android.opengl.GLES20.glUseProgram
import android.opengl.GLES20.glVertexAttribPointer
import android.util.Log
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

open class ARInput(val vertexShader: String = VERTEX_SHADER,
                   val fragmentShader: String = FRAGMENT_SHADER) {
    companion object {

        const val VERTEX_SHADER = """
            attribute vec4 position;
            attribute vec4 coordinate;
            varying vec2 textureCoordinate;
            void main() {
                gl_Position = position;
                textureCoordinate = coordinate.xy;
            }
            """

        const val FRAGMENT_SHADER = """
            varying highp vec2 textureCoordinate;
            uniform sampler2D texture;
            void main() {
                gl_FragColor = texture2D(texture, textureCoordinate);
            }
            """

        private const val TAG = "ARInput"

        private val COORDINATE = floatArrayOf(
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        )

        private val POSITION = floatArrayOf(
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f
        )
    }

    private var program = 0
    private var textureId = 0
    private var textureIndex = -1
    private var textureTarget = GL_TEXTURE_2D
    private var textureLocation = 0
    private var positionLocation = 0
    private var coordinateLocation = 0
    private var initialized = false

    private var render: ARRender? = null

    protected var width = 0
    protected var height = 0
    protected var positionBuffer: FloatBuffer? = null
    protected var coordinateBuffer: FloatBuffer? = null

    fun init(id: Int, index: Int) {
        Log.i(TAG, "[init]id=$id,index=$index")
        textureId = id
        textureIndex = index
        if (!initialized) {
            program = createProgram(vertexShader.trimIndent(), fragmentShader.trimIndent())
            if (program <= 0) {
                Log.e(TAG, "[init] Create program Error($program), please check your shader.")
            }

            textureLocation = glGetUniformLocation(program, "texture")
            positionLocation = glGetAttribLocation(program, "position")
            coordinateLocation = glGetAttribLocation(program, "coordinate")
            Log.i(TAG, "[init] $textureLocation, $positionLocation, $coordinateLocation")

            onInit()

            if (positionBuffer == null) {
                positionBuffer = ByteBuffer.allocateDirect(POSITION.size * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer()
                positionBuffer?.put(POSITION)
                positionBuffer?.position(0)
            }
            if (coordinateBuffer == null) {
                coordinateBuffer = ByteBuffer.allocateDirect(COORDINATE.size * 4)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer()
                coordinateBuffer?.put(COORDINATE)
                coordinateBuffer?.position(0)
            }
            initialized = true
        }
    }

    fun destroy() {
        glDeleteProgram(program)
        onDestroy()
        initialized = false
    }

    fun onSizeChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun draw(gl: GL10) {
        glUseProgram(program)

        glEnableVertexAttribArray(positionLocation)
        glEnableVertexAttribArray(coordinateLocation)

        glVertexAttribPointer(positionLocation, 2, GL_FLOAT, false, 0, positionBuffer)
        glVertexAttribPointer(coordinateLocation, 2, GL_FLOAT, false, 0, coordinateBuffer)

        if (textureId != -1) {
            glActiveTexture(GL_TEXTURE0 + textureIndex)
            glBindTexture(textureTarget, textureId)
            glUniform1i(textureLocation, textureIndex)
        }

        onDrawArraysBefore(gl)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        onDrawArraysAfter(gl)

        glDisableVertexAttribArray(positionLocation)
        glDisableVertexAttribArray(coordinateLocation)
    }

    fun setRender(render: ARRender) {
        this.render = render
    }

    fun requestRender() {
        render?.requestRender()
    }

    fun isInitialized() = initialized
    fun onTouchEvent(event: MotionEvent) = false
    protected fun getProgram() = program
    protected fun getTextureId() = textureId
    protected fun getTextureIndex() = textureIndex
    protected fun getTextureTarget() = textureTarget

    open protected fun onInit() {}
    open protected fun onDestroy() {}
    open protected fun onDrawArraysBefore(gl: GL10) {}
    open protected fun onDrawArraysAfter(gl: GL10) {}

    private fun createProgram(vertexShader: String, fragmentShader: String): Int {
        val vShaderId = glCreateShader(GL_VERTEX_SHADER)
        val fShaderId = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(vShaderId, vertexShader)
        glShaderSource(fShaderId, fragmentShader)
        glCompileShader(vShaderId)
        glCompileShader(fShaderId)

        val program = glCreateProgram()
        glAttachShader(program, vShaderId)
        glAttachShader(program, fShaderId)
        glLinkProgram(program)

        glDeleteShader(vShaderId)
        glDeleteShader(fShaderId)

        return program
    }
}