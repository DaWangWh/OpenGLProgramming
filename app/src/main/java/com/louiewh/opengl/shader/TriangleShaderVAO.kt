package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.GLES30.GL_ELEMENT_ARRAY_BUFFER
import android.opengl.GLES30.GL_STATIC_DRAW
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

class TriangleShaderVAO: TriangleShader() {

    private var VBO = 0
    private var VAO = 0
    private var EBO = 0

    private var mGreenValue = 0f
    private var mCurrentTime = System.currentTimeMillis()

    override fun onInitGLES(program: Int) {
        super.onInitGLES(program)
        initVBO()
        initEBO()
        initVAO()
    }


    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(getShaderProgram())

        glDrawColor()

        GLES30.glBindVertexArray(VAO)
        // GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 3)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glBindVertexArray(GLES30.GL_NONE)
    }

    private fun glDrawColor() {
        val timeValue: Long = System.currentTimeMillis()
        if (timeValue - mCurrentTime > 100) {
            mCurrentTime = timeValue
            mGreenValue = (sin(timeValue.toDouble()) / 2 + 0.5f).toFloat()
            GLES30.glUniform4f(uColor, 0.0f, mGreenValue, 0.0f, 1.0f)
            // Log.e("GLES", "greenVale: $mGreenValue")
        }
    }

    private fun initVBO() {
        val vertices = getVertices()

        val intArray = IntArray(1)
        GLES30.glGenBuffers(1, intArray, 0)
        VBO = intArray[0]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.capacity() * VERTICES_FLOAT_SIZE, vertices, GLES30.GL_STREAM_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)
    }

    private fun initEBO(){
        val indices = getIndex()

        val intArray = IntArray(1)
        GLES30.glGenBuffers(1, intArray, 0)
        EBO = intArray[0]

        GLES30.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO)
        GLES30.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.capacity()* Int.SIZE_BYTES, indices, GL_STATIC_DRAW)
        GLES30.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, GLES30.GL_NONE)
    }

    private fun initVAO() {
        val intArray = IntArray(1)
        GLES30.glGenVertexArrays(1, intArray, 0)
        VAO = intArray[0]

        //绑定VAO对象, VBO 的操作记录在VAO中
        GLES30.glBindVertexArray(VAO)

        // VBO bind
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO)
        // EBO bind
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, EBO)

        GLES30.glVertexAttribPointer(vPosition, 2, GLES30.GL_FLOAT, false, 2 * VERTICES_FLOAT_SIZE, 0)
        GLES30.glEnableVertexAttribArray(vPosition)

        //解绑
        GLES30.glBindVertexArray(GLES30.GL_NONE)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GLES30.GL_NONE)
        // finish VBO bind
    }

    override fun getVertices(): FloatBuffer {
        val vertices = floatArrayOf(
            0.5f, 0.5f,    // 右上角
            0.5f, -0.5f,   // 右下角
            -0.5f, -0.5f,  // 左下角
            -0.5f, 0.5f    // 左上角
        )

        // vertices.length*4是因为一个float占四个字节
        val vertexBuffer = ByteBuffer.allocateDirect(vertices.size * VERTICES_FLOAT_SIZE)//4个字节
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
            .apply {
                position(0)
            }

        return vertexBuffer
    }

    override fun onDestroyGLES() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(VAO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(VBO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(EBO), 0)
    }

    private fun getIndex():IntBuffer{
        val indices = intArrayOf(
            // 注意索引从0开始!
            // 此例的索引(0,1,2,3)就是顶点数组vertices的下标，
            // 这样可以由下标代表顶点组合成矩形

            0, 1, 3, // 第一个三角形
            1, 2, 3  // 第二个三角形
        )

        val eboBuffer = ByteBuffer.allocateDirect(indices.size * Int.SIZE_BYTES)//4个字节
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(indices)
            .apply {
                position(0)
            }

        return eboBuffer
    }
}