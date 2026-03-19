package com.louiewh.opengl.shader

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.louiewh.opengl.GlslAssetsUtil
import com.louiewh.opengl.ContextUtil
import com.louiewh.opengl.GlesSurfaceView
import javax.microedition.khronos.opengles.GL10

abstract class BaseShader {
    private companion object {
        val TAG:String = "BaseShader"
    }

    val VERTICES_FLOAT_SIZE:Int by lazy { Float.SIZE_BYTES }

    private var mProgram:Int = 0

    abstract fun onInitGLES(program: Int)

    abstract fun onDestroyGLES()

    abstract fun getVertexSource():String

    abstract fun getFragmentSource():String

    abstract fun  onDrawFrame(gl: GL10?)

    /**
     * 当shader被切换走时调用，用于停止后台线程等资源
     * 子类如YUVRender需要重写此方法来停止IO线程
     */
    open fun onDeactivate() {
        // 默认空实现
    }

    /**
     * 当shader被切换回来时调用，用于重新启动后台线程等资源
     * 子类如YUVRender需要重写此方法来重启IO线程
     */
    open fun onActivate() {
        // 默认空实现
    }

    open fun onSetGLSurfaceView(glSurfaceView:GLSurfaceView){

    }

    open fun onSetGlesSurfaceView(glesSurfaceView: GlesSurfaceView){

    }

    open fun onSurfaceChanged(gl: GL10?, width: Int, height: Int){
        GLES30.glViewport(0, 0, width, height)
    }

    /**
     * 获取shader需要的渲染模式
     * 默认为CONTINUOUSLY（连续渲染），适用于动画效果
     * YUV shader 等需要 WHEN_DIRTY（按需渲染）
     */
    open fun getRenderMode(): Int = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    fun onSurfaceCreated(){
        Log.d("BaseShader", "onSurfaceCreated start for ${this.javaClass.simpleName}")
        GLES30.glClearColor(0.0f, 0F, 0F, 1.0f)

        mProgram = createProgram(getVertexSource(), getFragmentSource())
        Log.d("BaseShader", "Program created: $mProgram")
        onInitGLES(mProgram)
        Log.d("BaseShader", "onInitGLES done")
    }

    fun destroyGLES(){
        GLES30.glDeleteProgram(mProgram)
        onDestroyGLES()
    }

    /**
     * 获取OpenGL program ID，用于glUseProgram()切换
     */
    fun getProgramId(): Int = mProgram

    protected open fun getShaderProgram():Int{
        return mProgram
    }

    protected fun readGlslSource(name:String):String{
        return GlslAssetsUtil.readGlslSource(ContextUtil.getContext(), "glsl/$name")
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) {
            return 0
        }
        val pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (pixelShader == 0) {
            return  0
        }
        var program = GLES30.glCreateProgram()
        if (program != 0) {
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, pixelShader)
            GLES30.glLinkProgram(program)
        }

        program = checkProgramiv(program)

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(pixelShader)

        return program
    }

    private fun loadShader(shaderType: Int, sourceCode: String): Int {
        var shader = GLES30.glCreateShader(shaderType)
        if (shader != 0) {
            GLES30.glShaderSource(shader, sourceCode)
            GLES30.glCompileShader(shader)
        }

        shader = checkShaderiv(shader, shaderType)
        return shader
    }

    private fun checkShaderiv(shader: Int, shaderType: Int): Int {
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader $shaderType:")
            Log.e(TAG, GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    private fun checkProgramiv(program: Int): Int {
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: ")
            Log.e(TAG, GLES30.glGetProgramInfoLog(program))
            GLES30.glDeleteProgram(program)

            return 0
        }
        return program
    }
}