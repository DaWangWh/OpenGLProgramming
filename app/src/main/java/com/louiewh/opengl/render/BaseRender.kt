package com.louiewh.opengl.render

import android.opengl.GLSurfaceView
import android.util.Log
import com.louiewh.opengl.GlesSurfaceView
import com.louiewh.opengl.ShaderManager
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class BaseRender : GLSurfaceView.Renderer {

    private val TAG = "BaseRender"

    private var pendingShaderName: String? = null
    private var _glesSurfaceView: GlesSurfaceView? = null

    open fun setGLSurfaceView(glSurfaceView: GLSurfaceView) {
        // 设置 OpenGL 版本(一定要设置)
        glSurfaceView.setEGLContextClientVersion(3)
        // 设置渲染器(后面会讲，可以理解成画笔)
        glSurfaceView.setRenderer(this)
        // 设置渲染模式为连续模式(会以 60 fps 的速度刷新)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    fun setGlesSurfaceView(glSurfaceView: GlesSurfaceView) {
        _glesSurfaceView = glSurfaceView
        // 设置渲染器(后面会讲，可以理解成画笔)
        glSurfaceView.setRenderer(this)
        // 设置渲染模式为连续模式(会以 60 fps 的速度刷新)
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated called")

        if (_glesSurfaceView != null) {
            // ShaderManager会自己判断是否需要重新初始化（上下文丢失后需要重建）
            ShaderManager.initAllShaders(_glesSurfaceView!!, 0, 0)

            Log.d(TAG, "ShaderManager initialized with ${ShaderManager.getShaderCount()} shaders")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")
        // 更新所有shader的尺寸
        ShaderManager.updateSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 处理切换请求
        pendingShaderName?.let { name ->
            ShaderManager.useShader(name)
            pendingShaderName = null
        }

        // 绘制当前shader
        ShaderManager.getCurrentShader()?.onDrawFrame(gl)
    }

    fun switchShader(name: String) {
        Log.d(TAG, "Requesting shader switch to: $name")
        pendingShaderName = name
    }

    fun destroyShader() {
        Log.d(TAG, "destroyShader called")
        ShaderManager.destroyAll()
    }
}