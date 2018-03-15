package com.foolment.arinput.core

import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_COLOR_ATTACHMENT0
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_FRAMEBUFFER
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES20.GL_RGBA
import android.opengl.GLES20.GL_SRC_ALPHA
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.GL_UNSIGNED_BYTE
import android.opengl.GLES20.glBindFramebuffer
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glDeleteFramebuffers
import android.opengl.GLES20.glDeleteTextures
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glFramebufferTexture2D
import android.opengl.GLES20.glGenFramebuffers
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexImage2D
import android.opengl.GLES20.glTexParameterf
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView.Renderer
import android.util.Log
import android.view.MotionEvent
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ARRender : Renderer {

    companion object {
        private const val TAG = "ARRender"
    }

    private var width = -1
    private var height = -1
    private var textureIds: IntArray? = null
    private var frameBuffers: IntArray? = null
    private var frameBufferTextureIds: IntArray? = null
    private var surfaceView: ARSurfaceView? = null

    private val inputs = ArrayList<ARInput>()
    private val filters = ArrayList<ARInput>()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "[onSurfaceCreated]")
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        textureIds = IntArray(inputs.size)
        glGenTextures(textureIds!!.size, textureIds, 0)
        inputs.forEachIndexed { index, input ->
            input.setRender(this)
            input.init(textureIds!![index], index)
        }

        frameBuffers = IntArray(filters.size)
        frameBufferTextureIds = IntArray(filters.size)
        glGenFramebuffers(frameBufferTextureIds!!.size, frameBuffers, 0)
        glGenTextures(frameBufferTextureIds!!.size, frameBufferTextureIds, 0)
        filters.forEachIndexed { index, filter ->
            filter.init(frameBufferTextureIds!![index], 0)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "[onSurfaceChanged] width=$width,height=$height")
        this.width = width
        this.height = height
        glViewport(0, 0, width, height)
        inputs.forEach { it.onSizeChanged(width, height) }
        filters.forEachIndexed { index, filter ->
            filter.onSizeChanged(width, height)

            glBindTexture(GL_TEXTURE_2D, frameBufferTextureIds!![index])
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR.toFloat())
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR.toFloat())
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE.toFloat())
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE.toFloat())

            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers!![index])
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D,
                    frameBufferTextureIds!![index], 0)

            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            glBindTexture(GL_TEXTURE_2D, 0)
        }
    }

    override fun onDrawFrame(gl: GL10) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        if (frameBuffers != null && filters.size > 0) {
            // Draw with filters
            glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers!![0])
            glClearColor(0f, 0f, 0f, 0f)
            inputs.forEach { it.draw(gl) }
            glBindFramebuffer(GL_FRAMEBUFFER, 0)

            for (i in 0 until filters.size) {
                if (i < filters.size - 1) {
                    // Draw on the buffer
                    glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers!![i + 1])
                    glClearColor(0f, 0f, 0f, 0f)
                    filters[i].draw(gl)
                    glBindFramebuffer(GL_FRAMEBUFFER, 0)
                } else {
                    // Draw on the screen
                    filters[i].draw(gl)
                }
            }
        } else {
            // Draw without filters
            inputs.forEach { it.draw(gl) }
        }
    }

    fun setSurfaceView(surfaceView: ARSurfaceView) {
        this.surfaceView = surfaceView
    }

    fun addInput(input: ARInput) {
        inputs.add(input)
    }

    fun addFilter(filter: ARInput) {
        filters.add(filter)
    }

    fun requestRender() {
        surfaceView?.requestRender()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        for (input in inputs) {
            if (input.onTouchEvent(event)) return true
        }
        return false
    }

    fun destroy() {
        inputs.forEach { it.destroy() }
        filters.forEach { it.destroy() }
        if (textureIds != null) glDeleteTextures(textureIds!!.size, textureIds, 0)
        if (frameBuffers != null) glDeleteFramebuffers(frameBuffers!!.size, frameBuffers, 0)
        if (frameBufferTextureIds != null) {
            glDeleteTextures(frameBufferTextureIds!!.size, frameBufferTextureIds, 0)
        }
        frameBuffers = null
        frameBufferTextureIds = null
    }
}