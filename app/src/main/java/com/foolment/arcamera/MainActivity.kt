package com.foolment.arcamera

import android.app.Activity
import android.os.Bundle
import com.foolment.arinput.core.ARRender
import com.foolment.arinput.core.ARSurfaceView
import com.foolment.arinput.toolbox.CameraInput
import com.foolment.arinput.toolbox.ImageInput
import com.foolment.arinput.toolbox.PixelFilter

class MainActivity : Activity() {

    private var surfaceView: ARSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val render = ARRender()
        surfaceView = ARSurfaceView(this)
        surfaceView?.setRender(render)

        render.addInput(CameraInput(this))
        render.addInput(ImageInput(this, R.mipmap.ic_launcher))
        render.addFilter(PixelFilter(20f))

        setContentView(surfaceView)
    }

    override fun onDestroy() {
        super.onDestroy()
        surfaceView?.destroy()
    }
}
