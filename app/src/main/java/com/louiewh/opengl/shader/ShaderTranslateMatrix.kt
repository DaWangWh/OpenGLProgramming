package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.Matrix
import javax.microedition.khronos.opengles.GL10

class ShaderTranslateMatrix: ShaderOrthoMatrix() {

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

        super.onSurfaceChanged(gl, width, height)
        Matrix.translateM(mvpMatrix,0,0.5f,0.0f,0f)
        GLES30.glUseProgram(getShaderProgram())
        GLES30.glUniformMatrix4fv(uMatrix,1,false, mvpMatrix,0)
    }
}