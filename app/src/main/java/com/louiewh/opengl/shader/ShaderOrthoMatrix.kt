package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.opengles.GL10

open class ShaderOrthoMatrix: ShaderMatrix() {

    protected val mvpMatrix = getUnitMatrix()
    private val TAG = "ShaderOrthoMatrix"

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        super.onSurfaceChanged(gl, width, height)

        val aspectRatio = if (width > height) {
            width.toFloat() / height
        } else {
            height.toFloat() / width
        }

        // 重置矩阵为单位矩阵
        System.arraycopy(getUnitMatrix(), 0, mvpMatrix, 0, 16)

        // 1. 矩阵数组
        // 2. 结果矩阵起始的偏移量
        // 3. left：x的最小值
        // 4. right：x的最大值
        // 5. bottom：y的最小值
        // 6. top：y的最大值
        // 7. near：z的最小值
        // 8. far：z的最大值
        // 由于是正交矩阵，所以偏移量为0，near 和 far 也不起作用
        if (width > height){
            Matrix.orthoM(mvpMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
        }else{
            Matrix.orthoM(mvpMatrix,0, -1f, 1f, -aspectRatio, aspectRatio, -1f, 1f)
        }

        //更新 matrix 的值，即把 UnitMatrix 值，更新到 uMatrix 这个索引
        GLES30.glUseProgram(getShaderProgram())
        GLES30.glUniformMatrix4fv(uMatrix,1,false, mvpMatrix,0)
        Log.d(TAG, "Matrix set complete")
    }

    private fun getUnitMatrix() =  floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )
}