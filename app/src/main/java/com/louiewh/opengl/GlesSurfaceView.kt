package com.louiewh.opengl

import android.content.Context
import android.opengl.EGL14
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

class GlesSurfaceView : SurfaceView, SurfaceHolder.Callback {
    private var mGlesThread: GlesThread? = null
    private var mRender: GLSurfaceView.Renderer? = null
    private var mRenderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    private val TAG = "GlesSurfaceView"

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        holder.addCallback(this)
    }

    fun setRenderer(renderer: GLSurfaceView.Renderer) {
        mRender = renderer
    }

    fun setRenderMode(mode: Int) {
        mRenderMode = mode
        mGlesThread?.setRenderMode(mode)
    }

    fun requestRender() {
        mGlesThread?.requestRender()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceCreated")
        if (mGlesThread == null) {
            mGlesThread = GlesThread(holder.surface)
            mGlesThread!!.start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.e(TAG, "surfaceChanged width:$width height:$height")
        mGlesThread?.onWindowResize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceDestroyed")

        mGlesThread?.exit()
        mGlesThread = null
    }


    inner class GlesThread : Thread {
        private lateinit var mEglHelper: EglHelper
        private var mSurface: Surface
        private var mWidth = 0
        private var mHeight = 0
        private var mRenderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        private var mRequestRender = false
        @Volatile
        private var mRunning = true
        private var isSizeChange = false
        private val renderLock = Object()

        constructor(surface: Surface) : super() {
            mWidth = 0
            mHeight = 0
            mRequestRender = true
            mRenderMode = this@GlesSurfaceView.mRenderMode
            mSurface = surface
        }

        override fun run() {
            mEglHelper = EglHelper()
            mEglHelper.initEgl(mSurface)
            Log.d(TAG, "Initial onSurfaceCreated")
            this@GlesSurfaceView.mRender?.onSurfaceCreated(mEglHelper.getGL(), mEglHelper.getEGLConfig())

            var frameCount = 0
            while (mRunning) {
                var needResize = false
                var width = 0
                var height = 0
                var shouldDraw = false
                var isContinuousMode = false
                var shouldExit = false

                synchronized(renderLock) {
                    while (
                        mRunning &&
                        !isSizeChange &&
                        !mRequestRender &&
                        mRenderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    ) {
                        try {
                            renderLock.wait()
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }

                    if (!mRunning) {
                        shouldExit = true
                    }

                    isContinuousMode = mRenderMode == GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    if (isSizeChange) {
                        needResize = true
                        width = mWidth
                        height = mHeight
                        isSizeChange = false
                    }
                    shouldDraw = isContinuousMode || mRequestRender || needResize
                    mRequestRender = false
                }

                if (shouldExit) {
                    break
                }

                if (needResize) {
                    Log.d(TAG, "onSurfaceChanged due to size change: $width x $height")
                    this@GlesSurfaceView.mRender?.onSurfaceChanged(mEglHelper.getGL(), width, height)
                }

                if (shouldDraw) {
                    this@GlesSurfaceView.mRender?.onDrawFrame(mEglHelper.getGL())
                    this.mEglHelper.swapBuffers()
                    frameCount++
                    if (frameCount <= 5) {
                        Log.d(TAG, "Frame $frameCount rendered")
                    }
                }

                if (isContinuousMode) {
                    try {
                        sleep(16)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }

            mEglHelper.destroy()
        }

        fun onWindowResize(w: Int, h: Int) {
            Log.d(TAG, "onWindowResize: $w x $h")
            synchronized(renderLock) {
                mWidth = w
                mHeight = h
                if (w > 0 && h > 0) {
                    isSizeChange = true
                    renderLock.notifyAll()
                }
            }
        }

        fun exit() {
            synchronized(renderLock) {
                mRunning = false
                renderLock.notifyAll()
            }
            try {
                if (Thread.currentThread() !== this) {
                    join()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "exit", e)
                Thread.currentThread().interrupt()
            }
            mGlesThread = null
        }

        fun setRenderMode(mode: Int) {
            synchronized(renderLock) {
                mRenderMode = mode
                renderLock.notifyAll()
            }
        }

        fun requestRender() {
            synchronized(renderLock) {
                mRequestRender = true
                renderLock.notifyAll()
            }
        }
    }

    inner class EglHelper {
        private var EGL: EGL10? = null
        private var GL: GL10? = null

        private var eglConfig: EGLConfig? = null
        private var eglDisplay: EGLDisplay? = null
        private var eglSurface: EGLSurface? = null
        private var eglContext: EGLContext? = null

        private var surface: Surface? = null

        fun getGL(): GL10 {
            return GL!!
        }

        fun getEGLConfig(): EGLConfig {
            return eglConfig!!
        }

        fun initEgl(surface: Surface) {
            //1、得到Egl实例：
            val egl = EGLContext.getEGL() as EGL10

            //2、得到默认的显示设备（就是窗口）
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            if (display === EGL10.EGL_NO_DISPLAY) {
                throw RuntimeException("eglGetDisplay error ${egl.eglGetError()}")
            }

            //3、初始化默认显示设备
            val displayVersions = IntArray(2)
            if (!egl.eglInitialize(display, displayVersions)) {
                throw RuntimeException("eglInitialize error ${egl.eglGetError()}")
            }

            // 4、设置显示设备的属性
            val attr = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 8,
                EGL10.EGL_STENCIL_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_NONE
            )

            // 5、从系统中获取对应属性的配置
            val num_config = IntArray(1)
            if (!egl.eglChooseConfig(display, attr, null, 1, num_config)) {
                throw RuntimeException("eglChooseConfig error ${egl.eglGetError()}")
            }

            val numConfigs = num_config[0]
            if (numConfigs <= 0) {
                throw RuntimeException("No configs match configSpec error ${egl.eglGetError()}")
            }

            val configs = arrayOfNulls<EGLConfig>(numConfigs)
            if (!egl.eglChooseConfig(display, attr, configs, numConfigs, num_config)) {
                throw RuntimeException("eglChooseConfig#2 error ${egl.eglGetError()}")
            }

            //6、创建渲染的Surface
            val eglSurface = egl.eglCreateWindowSurface(display, configs[0], surface, null)

            //7、创建EglContext，3 表示OpenGL 版本号
            val attrib_list = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL10.EGL_NONE
            )
            val eglContext = egl.eglCreateContext(display, configs[0], EGL10.EGL_NO_CONTEXT, attrib_list)



            //8、绑定EglContext和Surface到显示设备中
            if (!egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext)) {
                throw RuntimeException("eglMakeCurrent error ${egl.eglGetError()}")
            }

            this.eglDisplay = display
            this.eglSurface = eglSurface
            this.eglContext = eglContext
            this.GL = eglContext.gl as GL10
            this.eglConfig = configs[0]
            this.surface = surface
            this.EGL = egl
        }

        fun swapBuffers() {
            EGL?.eglSwapBuffers(eglDisplay, eglSurface)
        }

        fun destroy() {
            EGL?.apply {
                eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)

                eglDestroySurface(eglDisplay, eglSurface)
                eglSurface = null

                eglDestroyContext(eglDisplay, eglContext)
                eglContext = null

                eglTerminate(eglDisplay)
                eglDisplay = null

                surface?.release()

                EGL = null
            }
        }
    }
}
