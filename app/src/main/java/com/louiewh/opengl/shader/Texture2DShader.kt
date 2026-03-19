package com.louiewh.opengl.shader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.util.Log
import com.louiewh.opengl.ContextUtil
import com.louiewh.opengl.R
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL10

/**
 * 1. bitmap copy 后buf rewind
 * 2. glGenTextures and glBindTexture
 * 3. 图片反转问题， 修改纹理坐标
 */

open class Texture2DShader :BaseShader() {
    private var VBO = 0
    private var VAO = 0
    private var EBO = 0
    private var  mTextureId = 0

    private var vPosition = 0
    private var vColor = 0
    private var vTexCoord = 0
    private var vSampler2D = 0

    override fun onInitGLES(program: Int) {
        vPosition  = GLES30.glGetAttribLocation(program, "aPos")
        vColor     = GLES30.glGetAttribLocation(program, "aColor")
        vTexCoord  = GLES30.glGetAttribLocation(program, "aTexCoord")
        vSampler2D = GLES30.glGetUniformLocation(program, "ourTexture")

        Log.e("Gles", "onInitGLES ->vPosition: $vPosition vColor: $vColor vTexCoord: $vTexCoord")

        initVBO()
        initEBO()
        initVAO()
        initTexture()
        GLES30.glUseProgram(getShaderProgram())
    }

    override fun onDestroyGLES() {
        GLES30.glDeleteBuffers(1, intArrayOf(VAO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(VBO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(EBO), 0)
        GLES30.glDeleteTextures(1, intArrayOf(mTextureId), 0)
    }

    override fun getVertexSource(): String {
        return readGlslSource("Texture2D.vert")
    }

    override fun getFragmentSource(): String {
        return readGlslSource("Texture2D.frag")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)
        GLES30.glUniform1i(vSampler2D, 0)

        GLES30.glBindVertexArray(VAO)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glBindVertexArray(GLES30.GL_NONE)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
    }

    private fun initVBO() {
        val vertices = getVertices()

        val intArray = IntArray(1)
        GLES30.glGenBuffers(intArray.size, intArray, 0)
        VBO = intArray[0]

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, VBO)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.capacity() * VERTICES_FLOAT_SIZE, vertices, GLES30.GL_STREAM_DRAW)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)
    }

    private fun initEBO(){
        val indices = getIndex()

        val intArray = IntArray(1)
        GLES30.glGenBuffers(intArray.size, intArray, 0)
        EBO = intArray[0]

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, EBO)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.capacity()* Int.SIZE_BYTES, indices,
            GLES30.GL_STATIC_DRAW
        )
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GLES30.GL_NONE)
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

        GLES30.glVertexAttribPointer(vPosition, 3, GLES30.GL_FLOAT, false, 8 * VERTICES_FLOAT_SIZE, 0)
        GLES30.glVertexAttribPointer(vColor,    3, GLES30.GL_FLOAT, false, 8 * VERTICES_FLOAT_SIZE, 3*VERTICES_FLOAT_SIZE)
        GLES30.glVertexAttribPointer(vTexCoord, 2, GLES30.GL_FLOAT, false, 8 * VERTICES_FLOAT_SIZE, 6*VERTICES_FLOAT_SIZE)

        GLES30.glEnableVertexAttribArray(vPosition)
        GLES30.glEnableVertexAttribArray(vColor)
        GLES30.glEnableVertexAttribArray(vTexCoord)

        //解绑
        GLES30.glBindVertexArray(GLES30.GL_NONE)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, GLES30.GL_NONE)
        // finish VBO bind
    }

    private fun getVertices(): FloatBuffer {
        val vertices = floatArrayOf(
            // ---- 位置 ----       ---- 颜色 ----     - 纹理坐标 -
            0.5f,  0.5f, 0.0f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f,   // 右上
            0.5f, -0.5f, 0.0f,   0.0f, 1.0f, 0.0f,   1.0f, 1.0f,   // 右下
            -0.5f, -0.5f, 0.0f,   0.0f, 0.0f, 1.0f,   0.0f, 1.0f,   // 左下
            -0.5f,  0.5f, 0.0f,   1.0f, 1.0f, 0.0f,   0.0f, 0.0f    // 左上
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

    private fun getIndex(): Buffer {
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
            .run {
                position(0)
            }

        return eboBuffer
    }

    private fun initTexture(){
        val intArray = IntArray(1)
        GLES30.glGenTextures(intArray.size, intArray, 0)
        mTextureId = intArray[0]

        var bitmap = loadImageData(ContextUtil.getContext())
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)

        // 为当前绑定的纹理对象设置环绕、过滤方式
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            bitmap.width,
            bitmap.height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            getBitmapPixels(bitmap)
        )

        // GLES30.glUniform1i(vSampler2D, 0)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
        bitmap.recycle()
    }

    private fun loadImageData(context:Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.rose)
    }

    private fun getBitmapPixels(bitmap:Bitmap):ByteBuffer{
        val buf = ByteBuffer.allocate(bitmap.byteCount).order(ByteOrder.nativeOrder())
        bitmap.copyPixelsToBuffer(buf)
        buf.rewind()

        return buf
    }
}