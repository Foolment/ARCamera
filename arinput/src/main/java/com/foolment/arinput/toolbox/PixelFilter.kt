package com.foolment.arinput.toolbox

import android.opengl.GLES20
import com.foolment.arinput.core.ARInput
import javax.microedition.khronos.opengles.GL10

class PixelFilter(private val pixel: Float) : ARInput(fragmentShader = PIXEL_FRAGMENT_SHADER) {

    companion object {
        private const val PIXEL_FRAGMENT_SHADER = """
            precision highp float;
            varying vec2 textureCoordinate;
            uniform float widthFactor;
            uniform float heightFactor;
            uniform sampler2D texture;
            uniform float pixel;
            void main() {
                vec2 uv  = textureCoordinate.xy;
                float dx = pixel * widthFactor;
                float dy = pixel * heightFactor;
                vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
                vec3 tc = texture2D(texture, coord).xyz;
                gl_FragColor = vec4(tc, 1.0);
            }
            """
    }

    private var pixelLocation = 0
    private var widthFactorLocation = 0
    private var heightFactorLocation = 0

    override fun onInit() {
        pixelLocation = GLES20.glGetUniformLocation(getProgram(), "pixel")
        widthFactorLocation = GLES20.glGetUniformLocation(getProgram(), "widthFactor")
        heightFactorLocation = GLES20.glGetUniformLocation(getProgram(), "heightFactor")
    }

    override fun onDrawArraysBefore(gl: GL10) {
        super.onDrawArraysBefore(gl)
        GLES20.glUniform1f(widthFactorLocation, 1.0f / width)
        GLES20.glUniform1f(heightFactorLocation, 1.0f / height)
        GLES20.glUniform1f(pixelLocation, pixel)
    }
}