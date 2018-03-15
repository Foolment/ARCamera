package com.foolment.arinput.core

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class ARSurfaceView : GLSurfaceView {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet) : super(ctx, attrs)

    private var render: ARRender? = null

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.RGBA_8888)
    }

    fun destroy() {
        render?.destroy()
    }

    fun setRender(render: ARRender) {
        this.render = render
        this.render?.setSurfaceView(this)
        setRenderer(this.render)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return render?.onTouchEvent(event) == true || super.onTouchEvent(event)
    }
}
