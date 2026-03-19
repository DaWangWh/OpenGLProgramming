package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.GLES30.GL_NONE
import javax.microedition.khronos.opengles.GL10

class TriangleShaderVBO: TriangleShader() {

    private var VBO = 0

    override fun onInitGLES(program: Int) {
        super.onInitGLES(program)
        initVBO()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(getShaderProgram())

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO)
        GLES30.glVertexAttribPointer(vPosition, 2, GLES30.GL_FLOAT, false, 2 * VERTICES_FLOAT_SIZE, 0)
        GLES30.glEnableVertexAttribArray(vPosition)
        GLES30.glUniform4f(uColor, 1f, 0.5f, 0.5f, 1.0f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 3)

        GLES30.glDisableVertexAttribArray(vPosition)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)
    }

    override fun onDestroyGLES() {
        GLES30.glDeleteBuffers(1, intArrayOf(VBO), 0)
    }

    private fun initVBO() {

        val intArray = IntArray(1)
        GLES30.glGenBuffers(1, intArray, 0)
        VBO = intArray[0]

        val vertices = getVertices()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.capacity()*VERTICES_FLOAT_SIZE, vertices, GLES30.GL_STREAM_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GL_NONE)
    }
}
