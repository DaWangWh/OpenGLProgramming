package com.louiewh.opengl.shader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.Matrix
import com.louiewh.opengl.ContextUtil
import com.louiewh.opengl.R
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10

/**
 *
 */
class Texture3DShader :BaseShader() {
    private var VBO = 0
    private var VAO = 0
    private var EBO = 0
    private var  mTextureId = 0

    private var vPosition = 0
    private var vTexCoord = 0
    private var vSampler2D = 0

    private var uMatrix = 0

    override fun onInitGLES(program: Int) {
        vPosition  = GLES30.glGetAttribLocation(program, "aPos")
        vTexCoord  = GLES30.glGetAttribLocation(program, "aTexCoord")
        vSampler2D = GLES30.glGetUniformLocation(program, "ourTexture")
        uMatrix   = GLES30.glGetUniformLocation(program, "uMatrix")

        initVBO()
        initEBO()
        initVAO()
        initTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)

        if (width <= 0 || height <= 0) {
            return
        }

        val aspectRatio = width.toFloat() / height.toFloat()

        val modelMatrix = getUnitMatrix()
        val viewMatrix = getUnitMatrix()
        val projectionMatrix = getUnitMatrix()
        val mvpMatrix = getUnitMatrix()

        Matrix.rotateM(modelMatrix, 0, -28f, 1f, 0f, 0f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.perspectiveM(projectionMatrix, 0, 45f, aspectRatio, 0.1f, 100f)

        Matrix.multiplyMM(mvpMatrix,0, viewMatrix,0, modelMatrix,0)
        Matrix.multiplyMM(mvpMatrix,0, projectionMatrix,0, mvpMatrix,0)

        GLES30.glUseProgram(getShaderProgram())
        GLES30.glUniformMatrix4fv(uMatrix,1,false, mvpMatrix,0)
    }

    override fun onDestroyGLES() {
        GLES30.glDeleteVertexArrays(1, intArrayOf(VAO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(VBO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(EBO), 0)
        GLES30.glDeleteTextures(1, intArrayOf(mTextureId), 0)
    }

    override fun getVertexSource(): String {
        return readGlslSource("Texture3D.vert")
    }

    override fun getFragmentSource(): String {
        return readGlslSource("Texture3D.frag")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(getShaderProgram())
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId)
        GLES30.glUniform1i(vSampler2D, 0)

        GLES30.glBindVertexArray(VAO)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, 6, GLES30.GL_UNSIGNED_INT, 0)

        GLES30.glBindVertexArray(GLES30.GL_NONE)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
    }

    override fun onActivate() {
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
    }

    private fun getUnitMatrix() =  floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

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
        GLES30.glVertexAttribPointer(vTexCoord, 2, GLES30.GL_FLOAT, false, 8 * VERTICES_FLOAT_SIZE, 6*VERTICES_FLOAT_SIZE)

        GLES30.glEnableVertexAttribArray(vPosition)
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
            0.42f,  0.62f,  0.10f,   1.0f, 0.0f, 0.0f,   1.0f, 0.0f,   // 右上
            0.56f, -0.62f,  0.32f,   0.0f, 1.0f, 0.0f,   1.0f, 1.0f,   // 右下
            -0.56f, -0.62f, 0.32f,   0.0f, 0.0f, 1.0f,   0.0f, 1.0f,   // 左下
            -0.42f,  0.62f, 0.10f,   1.0f, 1.0f, 0.0f,   0.0f, 0.0f    // 左上
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

    private fun getIndex(): IntBuffer {
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
