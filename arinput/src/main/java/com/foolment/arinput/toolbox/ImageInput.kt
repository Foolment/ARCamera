package com.foolment.arinput.toolbox

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE0
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glActiveTexture
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glTexParameterf
import android.opengl.GLUtils
import com.foolment.arinput.core.ARInput

class ImageInput(private val ctx: Context, private val resId: Int) : ARInput() {

    override fun onInit() {
        val bitmap = BitmapFactory.decodeResource(ctx.resources, resId)
        glActiveTexture(GL_TEXTURE0 + getTextureIndex())
        glBindTexture(GL_TEXTURE_2D, getTextureId())
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR.toFloat())
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR.toFloat())
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE.toFloat())
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE.toFloat())
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)
    }

}
