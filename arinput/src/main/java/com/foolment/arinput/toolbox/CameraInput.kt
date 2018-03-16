package com.foolment.arinput.toolbox

import android.content.Context
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.opengl.GLES11Ext
import android.util.Log
import android.view.WindowManager
import com.foolment.arinput.core.ARInput
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.opengles.GL10

class CameraInput(private var ctx: Context)
    : ARInput(fragmentShader = CAMERA_FRAGMENT_SHADER), OnFrameAvailableListener {

    companion object {
        private const val TAG = "CameraInput"
        private const val CAMERA_FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            varying highp vec2 textureCoordinate;
            uniform samplerExternalOES texture;
            void main() {
                 gl_FragColor = texture2D(texture, textureCoordinate);
            }
            """

        private val COORDINATE_ROTATED_0 = floatArrayOf(
                0.0f, 1.0f,
                1.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
        )

        private val COORDINATE_ROTATED_90 = floatArrayOf(
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                0.0f, 0.0f
        )

        private val COORDINATE_ROTATED_180 = floatArrayOf(
                1.0f, 0.0f,
                0.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f
        )

        private val COORDINATE_ROTATED_270 = floatArrayOf(
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        )
    }

    private var currCameraId = -1
    private var backCameraId = -1
    private var frontCameraId = -1
    private var previewWidth = 0
    private var previewHeight = 0
    private var needUpdate = false
    private var isCameraPreviewStarted = false
    private var camera: Camera? = null
    private var surfaceTexture: SurfaceTexture? = null

    override fun onInit() {
        setTextureTarget(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    }

    override fun onDestroy() {
        needUpdate = false
        surfaceTexture?.release()
        camera?.stopPreview()
        camera?.release()
    }

    override fun onSizeChanged(width: Int, height: Int) {
        super.onSizeChanged(width, height)
        startCameraPreview()
        adjustCameraDisplay()
    }

    override fun onDrawArraysBefore(gl: GL10) {
        synchronized(this) {
            if (needUpdate) {
                surfaceTexture?.updateTexImage()
                needUpdate = false
            }
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(this) {
            needUpdate = true
            requestRender()
        }
    }

    @Synchronized private fun startCameraPreview() {
        Log.i(TAG, "[startCameraPreview] width=$width,height=$height,start=$isCameraPreviewStarted")
        if (width > 0 && height > 0) {
            if (!isCameraPreviewStarted) {
                try {
                    initCameraId()
                    currCameraId = backCameraId
                    camera = Camera.open(currCameraId)
                    surfaceTexture = SurfaceTexture(getTextureId())
                    surfaceTexture?.setOnFrameAvailableListener(this)
                    camera?.setPreviewTexture(surfaceTexture)
                    isCameraPreviewStarted = true
                } catch (e: Exception) {
                    Log.e(TAG, "[startCameraPreview] $e")
                }
            }

            val param = camera?.parameters
            val sizes = param?.supportedPreviewSizes
            if (sizes != null && sizes.size > 0) {
                for (size in sizes) {
                    Log.i(TAG, "[startCameraPreview] SupportedPreviewSize(${size.width}x${size.height})")
                    val rotation = getCameraDisplayOrientation(currCameraId)
                    var pWidth = width
                    var pHeight = height
                    if (rotation == 90 || rotation == 270) {
                        pWidth = height
                        pHeight = width
                    }
                    if (size.width <= pWidth && size.height <= pHeight &&
                            (size.width >= previewWidth || size.height >= previewHeight)) {
                        previewWidth = size.width
                        previewHeight = size.height
                        Log.i(TAG, "[startCameraPreview] setPreviewSize(${size.width}x${size.height})")
                        param.setPreviewSize(previewWidth, previewHeight)
                    }
                }
            }
            camera?.parameters = param
            camera?.startPreview()
        }
    }

    private fun adjustCameraDisplay() {
        if (previewWidth == 0 || previewHeight == 0) {
            Log.e(TAG, "[adjustCameraDisplay]previewWidth=$previewWidth,previewHeight=$previewHeight")
            return
        }

        val rotation = getCameraDisplayOrientation(currCameraId)
        Log.i(TAG, "[adjustCameraDisplay] cameraId=$currCameraId,rotation=$rotation")

        var oWidth = width
        var oHeight = height
        if (rotation == 90 || rotation == 270) {
            oWidth = height
            oHeight = width
        }

        val radio = Math.max(oWidth * 1.0f / previewWidth, oHeight * 1.0f / previewHeight)
        val pWidth = previewWidth * radio
        val pHeight = previewHeight * radio
        val wRadio = pWidth / oWidth
        val hRadio = pHeight / oHeight

        val position = floatArrayOf(
                -1.0f * hRadio, -1.0f * wRadio,
                1.0f * hRadio, -1.0f * wRadio,
                -1.0f * hRadio, 1.0f * wRadio,
                1.0f * hRadio, 1.0f * wRadio
        )

        positionBuffer = ByteBuffer.allocateDirect(position.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        positionBuffer?.put(position)
        positionBuffer?.position(0)

        val coordinate = getCoordinateArray(rotation, currCameraId == frontCameraId)
        coordinateBuffer = ByteBuffer.allocateDirect(coordinate.size * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        coordinateBuffer?.put(coordinate)
        coordinateBuffer?.position(0)
    }

    private fun getCoordinateArray(rotation: Int, mirrored: Boolean): FloatArray {
        var coordinate = when ((rotation + 360) % 360) {
            90 -> COORDINATE_ROTATED_90
            180 -> COORDINATE_ROTATED_180
            270 -> COORDINATE_ROTATED_270
            else -> COORDINATE_ROTATED_0
        }
        if (mirrored) {
            coordinate = floatArrayOf(
                    coordinate[0], mirror(coordinate[1]),
                    coordinate[2], mirror(coordinate[3]),
                    coordinate[4], mirror(coordinate[5]),
                    coordinate[6], mirror(coordinate[7])
            )
        }
        return coordinate
    }

    private fun mirror(f: Float) = if (f == 0.0f) 1.0f else 0.0f

    private fun getCameraDisplayOrientation(cameraId: Int): Int {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val degree = wm.defaultDisplay.rotation * 90
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        return if (info.facing == CameraInfo.CAMERA_FACING_FRONT)
            (info.orientation + degree) % 360 else
            (info.orientation - degree + 360) % 360
    }

    private fun initCameraId() {
        for (i in 0 until Camera.getNumberOfCameras()) {
            val info = CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i
                Log.i(TAG, "[initCameraId] Back camera id: $backCameraId")
            } else if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i
                Log.i(TAG, "[initCameraId] Front camera id: $frontCameraId")
            }
        }
    }
}