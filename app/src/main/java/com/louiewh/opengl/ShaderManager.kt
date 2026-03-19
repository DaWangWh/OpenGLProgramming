package com.louiewh.opengl

import android.opengl.GLES30
import android.util.Log
import com.louiewh.opengl.shader.BaseShader

/**
 * Shader生命周期管理器
 *
 * 负责预创建所有shader program，并提供快速切换能力。
 * 切换shader时只需调用glUseProgram()，无需销毁重建。
 */
object ShaderManager {
    private const val TAG = "ShaderManager"

    private val shaders = mutableMapOf<String, BaseShader>()
    private var currentShaderName: String? = null
    private var preferredShaderName: String? = null  // 首次初始化时优先选择的shader
    private var currentWidth = 0
    private var currentHeight = 0
    private var glesSurfaceView: GlesSurfaceView? = null
    private var isInitialized = false
    private var currentTraceId: Long = 0
    private var firstFrameLogged = false

    /**
     * 设置首次初始化时优先选择的shader名称
     */
    fun setPreferredShaderName(name: String) {
        preferredShaderName = name
    }

    /**
     * 在onSurfaceCreated时调用，预创建所有shader program
     */
    fun initAllShaders(glSurfaceView: GlesSurfaceView, width: Int, height: Int) {
        // 记住当前shader名称（用于上下文丢失后恢复）
        val previousShaderName = currentShaderName

        // 如果已经初始化过，先清理旧资源（上下文丢失后旧资源无效）
        if (isInitialized) {
            Log.d(TAG, "Re-initializing shaders after context lost")
            currentShaderName?.let { currentName ->
                shaders[currentName]?.onDeactivate()
            }
            shaders.values.forEach { it.destroyGLES() }
            shaders.clear()
            currentShaderName = null
        }

        this.glesSurfaceView = glSurfaceView
        val initialSize = resolveSurfaceSize(width, height)
        this.currentWidth = initialSize.first
        this.currentHeight = initialSize.second

        Log.d(TAG, "Initializing all shaders, count: ${GlesConst.shaderArray.size}")
        GlesConst.shaderArray.forEach { meta ->
            val startTime = System.currentTimeMillis()
            val shader = meta.shaderFactory()
            shader.onSetGlesSurfaceView(glSurfaceView)
            shader.onSurfaceCreated()
            if (currentWidth > 0 && currentHeight > 0) {
                shader.onSurfaceChanged(null, currentWidth, currentHeight)
            }
            shaders[meta.renderName] = shader
            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Created shader '${meta.renderName}' in ${elapsed}ms, program=${shader.getProgramId()}")
        }

        isInitialized = true

        // 决定选择哪个shader
        val shaderToSelect = when {
            // 上下文丢失后恢复之前的shader
            previousShaderName != null && shaders.containsKey(previousShaderName) -> previousShaderName!!
            // 首次初始化，使用优先选择的shader
            preferredShaderName != null && shaders.containsKey(preferredShaderName) -> preferredShaderName!!
            // 默认选择第一个shader
            else -> GlesConst.shaderArray.first().renderName
        }

        setInitialShader(shaderToSelect)

        Log.d(TAG, "All shaders initialized successfully, selected shader: $shaderToSelect")
    }

    /**
     * 切换shader（在onDrawFrame中调用）
     */
    fun useShader(name: String, traceId: Long = 0): Boolean {
        Log.d(TAG, "trace=$traceId useShader start, target=$name, current=$currentShaderName, thread=${Thread.currentThread().name}")
        if (currentShaderName == name) {
            Log.d(TAG, "trace=$traceId Shader '$name' is already active")
            return false
        }

        val shader = shaders[name]
        if (shader == null) {
            Log.e(TAG, "trace=$traceId Shader '$name' not found")
            return false
        }

        // 停止当前shader的后台线程等资源
        currentShaderName?.let { currentName ->
            shaders[currentName]?.onDeactivate()
        }

        currentShaderName = name
        currentTraceId = traceId
        firstFrameLogged = false

        // 重置GL状态到默认值
        resetGLState()

        GLES30.glUseProgram(shader.getProgramId())

        // 切换时调用onSurfaceChanged，确保Matrix shader正确设置
        val surfaceSize = resolveSurfaceSize(currentWidth, currentHeight)
        currentWidth = surfaceSize.first
        currentHeight = surfaceSize.second
        if (currentWidth > 0 && currentHeight > 0) {
            Log.d(TAG, "trace=$traceId Applying surface size ${currentWidth}x${currentHeight} to shader '$name'")
            shader.onSurfaceChanged(null, currentWidth, currentHeight)
        } else {
            Log.w(TAG, "trace=$traceId No valid surface size for shader '$name'")
        }

        // 切换render mode
        glesSurfaceView?.setRenderMode(shader.getRenderMode())

        // 启动新shader的后台线程等资源
        shader.onActivate()

        Log.d(TAG, "trace=$traceId Switched to shader '$name', program=${shader.getProgramId()}, renderMode=${shader.getRenderMode()}")
        return true
    }

    /**
     * 重置GL状态到默认值
     */
    private fun resetGLState() {
        // 重置清除颜色为黑色
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }

    /**
     * 更新尺寸（在onSurfaceChanged中调用）
     */
    fun updateSize(width: Int, height: Int) {
        currentWidth = width
        currentHeight = height
        Log.d(TAG, "Updating size to ${width}x${height}")

        // 更新所有shader的尺寸
        shaders.values.forEach { it.onSurfaceChanged(null, width, height) }
    }

    /**
     * 获取当前shader
     */
    fun getCurrentShader(): BaseShader? {
        return shaders[currentShaderName]
    }

    /**
     * 获取当前shader名称
     */
    fun getCurrentShaderName(): String? = currentShaderName

    /**
     * 设置初始shader（初始化后调用）
     */
    fun setInitialShader(name: String) {
        if (shaders.containsKey(name)) {
            currentShaderName = name
            currentTraceId = 0
            firstFrameLogged = false
            shaders[name]?.let {
                GLES30.glUseProgram(it.getProgramId())
                val surfaceSize = resolveSurfaceSize(currentWidth, currentHeight)
                currentWidth = surfaceSize.first
                currentHeight = surfaceSize.second
                if (currentWidth > 0 && currentHeight > 0) {
                    it.onSurfaceChanged(null, currentWidth, currentHeight)
                }
                // 设置render mode
                glesSurfaceView?.setRenderMode(it.getRenderMode())
                // 启动shader的后台资源
                it.onActivate()
            }
            Log.d(TAG, "Initial shader set to '$name'")
        } else {
            Log.e(TAG, "Cannot set initial shader '$name' - not found")
        }
    }

    /**
     * 销毁所有shader
     */
    fun destroyAll() {
        Log.d(TAG, "Destroying all shaders")
        // 先停止当前shader的后台资源
        currentShaderName?.let { currentName ->
            shaders[currentName]?.onDeactivate()
        }
        shaders.values.forEach { it.destroyGLES() }
        shaders.clear()
        currentShaderName = null
        isInitialized = false
        glesSurfaceView = null
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * 获取已创建的shader数量
     */
    fun getShaderCount(): Int = shaders.size

    fun markFrameDrawn() {
        val shaderName = currentShaderName ?: return
        if (!firstFrameLogged) {
            firstFrameLogged = true
            Log.d(TAG, "trace=$currentTraceId First frame drawn for shader '$shaderName'")
        }
    }

    private fun resolveSurfaceSize(width: Int, height: Int): Pair<Int, Int> {
        if (width > 0 && height > 0) {
            return width to height
        }

        val viewWidth = glesSurfaceView?.width ?: 0
        val viewHeight = glesSurfaceView?.height ?: 0
        return viewWidth to viewHeight
    }
}
