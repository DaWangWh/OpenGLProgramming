package com.louiewh.opengl.render

import android.util.Log
import com.louiewh.opengl.ShaderManager

class GlesRender(private val renderName: String = "") : BaseRender() {

    init {
        // 设置优先选择的shader名称
        if (renderName.isNotEmpty()) {
            ShaderManager.setPreferredShaderName(renderName)
        }
        Log.d("GlesRender", "Created with preferred shader: $renderName")
    }
}