package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.louiewh.opengl.ContextUtil
import com.louiewh.opengl.GlesSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.thread

/**
 * ffmpeg 生成 YUV 数据  ffmpeg -i 1638871887691778.mp4  -s 568x320 -r 30 -pix_fmt yuv420p out.yuv
 * ffmpeg 播放 YUV 数据 ffplay -video_size 568x320  -i out.yuv
 */
open class YUVRender :BaseShader() {
    private var VBO = 0
    private var VAO = 0
    private var EBO = 0

    private var  yTextureId = 0
    private var  uTextureId = 0
    private var  vTextureId = 0

    private var yByteBuffer:ByteBuffer? = null
    private var uByteBuffer:ByteBuffer? = null
    private var vByteBuffer:ByteBuffer? = null

    private val wVideo = 288
    private val hVideo = 512
    private val YUV_FILE = "video_288_512.yuv"

    private var mStop = false
    private var isThreadRunning = false
    private lateinit var ioThread:Thread
    private lateinit var glesSurfaceView:GlesSurfaceView

    private var vPosition = 0
    private var vTexCoord = 0

    private var ySampler2D = 0
    private var uSampler2D = 0
    private var vSampler2D = 0


    override fun onSetGlesSurfaceView(glesSurfaceView: GlesSurfaceView) {
        glesSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
        this.glesSurfaceView = glesSurfaceView
    }

    override fun onSetGLSurfaceView(glSurfaceView: GLSurfaceView) {
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun getRenderMode(): Int = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    override fun onInitGLES(program: Int) {
        vPosition  = GLES30.glGetAttribLocation(program, "aPos")
        vTexCoord  = GLES30.glGetAttribLocation(program, "aTexCoord")

        ySampler2D = GLES30.glGetUniformLocation(program, "textureY")
        uSampler2D = GLES30.glGetUniformLocation(program, "textureU")
        vSampler2D = GLES30.glGetUniformLocation(program, "textureV")

        initVBO()
        initEBO()
        initVAO()

        yTextureId = initTexture(ySampler2D)
        uTextureId = initTexture(uSampler2D)
        vTextureId = initTexture(vSampler2D)

        // 不在这里启动IO线程，由onActivate()统一管理
    }

    private fun startIOThread() {
        if (isThreadRunning) {
            Log.d("YUVRender", "IO thread already running, skip start")
            return
        }
        mStop = false
        isThreadRunning = true
        ioThread = thread(name = "IOYUV") { readYuvData(YUV_FILE, wVideo, hVideo) }
        Log.d("YUVRender", "IO thread started")
    }

    private fun stopIOThread() {
        if (!isThreadRunning) {
            return
        }
        mStop = true
        isThreadRunning = false
        try {
            if (::ioThread.isInitialized) {
                ioThread.interrupt()
                ioThread.join(500)
            }
        } catch (e: Exception) {
            Log.e("YUVRender", "stopIOThread error", e)
        }
        Log.d("YUVRender", "IO thread stopped")
    }

    override fun onDeactivate() {
        Log.d("YUVRender", "onDeactivate: stopping IO thread")
        stopIOThread()
    }

    override fun onActivate() {
        Log.d("YUVRender", "onActivate: starting IO thread")
        startIOThread()
    }

    override fun onDestroyGLES() {
        stopIOThread()

        GLES30.glDeleteBuffers(1, intArrayOf(VAO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(VBO), 0)
        GLES30.glDeleteBuffers(1, intArrayOf(EBO), 0)

        GLES30.glDeleteTextures(1, intArrayOf(yTextureId), 0)
        GLES30.glDeleteTextures(1, intArrayOf(uTextureId), 0)
        GLES30.glDeleteTextures(1, intArrayOf(vTextureId), 0)
    }

    override fun getVertexSource(): String {
        return readGlslSource("YUV.vert")
    }

    override fun getFragmentSource(): String {
        return readGlslSource("YUV.frag")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glUseProgram(getShaderProgram())

        // Y
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, yTextureId)
        GLES30.glUniform1i(ySampler2D, 0)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_LUMINANCE,
            wVideo,
            hVideo,
            0,
            GLES30.GL_LUMINANCE,
            GLES30.GL_UNSIGNED_BYTE,
            yByteBuffer
        )

        // U
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, uTextureId)
        GLES30.glUniform1i(uSampler2D, 1)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_LUMINANCE,
            wVideo / 2,
            hVideo / 2,
            0,
            GLES30.GL_LUMINANCE,
            GLES30.GL_UNSIGNED_BYTE,
            uByteBuffer
        )

        // V
        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, vTextureId)
        GLES30.glUniform1i(vSampler2D, 2)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_LUMINANCE,
            wVideo / 2,
            hVideo / 2,
            0,
            GLES30.GL_LUMINANCE,
            GLES30.GL_UNSIGNED_BYTE,
            vByteBuffer
        )

        yByteBuffer?.clear()
        uByteBuffer?.clear()
        vByteBuffer?.clear()

        GLES30.glBindVertexArray(VAO)
        GLES30.glDrawElements(GLES30.GL_TRIANGLE_STRIP, 6, GLES30.GL_UNSIGNED_INT, 0)

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

        GLES30.glVertexAttribPointer(vPosition, 3, GLES30.GL_FLOAT, false, 5 * VERTICES_FLOAT_SIZE, 0)
        GLES30.glVertexAttribPointer(vTexCoord, 2, GLES30.GL_FLOAT, false, 5 * VERTICES_FLOAT_SIZE, 3*VERTICES_FLOAT_SIZE)

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
            // ---- 位置 ----           - 纹理坐标 -
            1.0f,    1.0f,     0.0f,      1.0f, 0.0f,   // 右上
            1.0f,   -1.0f,     0.0f,      1.0f, 1.0f,   // 右下
            -1.0f,  -1.0f,     0.0f,      0.0f, 1.0f,   // 左下
            -1.0f,   1.0f,     0.0f,      0.0f, 0.0f    // 左上
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

    private fun initTexture(id:Int):Int{
        val intArray = IntArray(1)
        GLES30.glGenTextures(intArray.size, intArray, 0)
        val textureId = intArray[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)

        // 为当前绑定的纹理对象设置环绕、过滤方式
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)

        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)

        return textureId
    }



    private fun readYuvData(yuvFile:String, w: Int, h: Int) {
        val input = ContextUtil.getContext().assets.open(yuvFile)
        //视频时 yuv420p ,4 个 y 共用一个 uv，先存储y，再u，和v
        val y = ByteArray(w * h)
        val u = ByteArray(w * h / 4)
        val v = ByteArray(w * h / 4)

        try {
            while (!mStop) {
                val readY = input.read(y)
                val readU = input.read(u)
                val readV = input.read(v)

                if (readY > 0 && readU > 0 && readV > 0) {
                    //从这里触发刷新
                    yByteBuffer = ByteBuffer.wrap(y)
                    uByteBuffer = ByteBuffer.wrap(u)
                    vByteBuffer = ByteBuffer.wrap(v)

                    //主动触发刷新
                    glesSurfaceView.requestRender()
                    //延时30ms，控制速度
                    Thread.sleep(30)
                } else {
                    return
                }
            }
        } catch (e: InterruptedException) {
            Log.d("YUVRender", "IO thread interrupted")
        } finally {
            input.close()
        }
    }
}