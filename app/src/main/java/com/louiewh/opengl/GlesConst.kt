package com.louiewh.opengl

import com.louiewh.opengl.shader.BaseShader
import com.louiewh.opengl.shader.ShaderOrthoMatrix
import com.louiewh.opengl.shader.ShaderRotateMatrix
import com.louiewh.opengl.shader.ShaderScaleMatrix
import com.louiewh.opengl.shader.ShaderTranslateMatrix
import com.louiewh.opengl.shader.ShaderStructArray
import com.louiewh.opengl.shader.Texture2DShader
import com.louiewh.opengl.shader.Texture3DClubShader
import com.louiewh.opengl.shader.Texture3DMutiClubShader
import com.louiewh.opengl.shader.Texture3DShader
import com.louiewh.opengl.shader.TriangleShader
import com.louiewh.opengl.shader.TriangleShaderVAO
import com.louiewh.opengl.shader.TriangleShaderVBO
import com.louiewh.opengl.shader.YUVRender
import com.louiewh.opengl.shader.YUVRenderColorReverse
import com.louiewh.opengl.shader.YUVRenderLuma
import com.louiewh.opengl.shader.YUVRenderSplit2
import com.louiewh.opengl.shader.YUVRenderSplit4

object GlesConst {

    data class ShaderMeta(
        val renderName: String,
        val shaderFactory: () -> BaseShader
    )

    val shaderArray: Array<ShaderMeta> = arrayOf(
        ShaderMeta("TriangleShader",          { TriangleShader() }),
        ShaderMeta("TriangleShaderVBO",       { TriangleShaderVBO() }),
        ShaderMeta("TriangleShaderVAO",       { TriangleShaderVAO() }),
        ShaderMeta("ShaderStructArray",       { ShaderStructArray() }),
        ShaderMeta("Texture2D",               { Texture2DShader() }),
        ShaderMeta("YUVRender",               { YUVRender() }),

        ShaderMeta("YUVRenderLuma",           { YUVRenderLuma() }),
        ShaderMeta("YUVRenderColorReverse",   { YUVRenderColorReverse() }),
        ShaderMeta("YUVRenderSplit",          { YUVRenderSplit4() }),
        ShaderMeta("YUVRenderLumaSplit2",     { YUVRenderSplit2() }),

        ShaderMeta("ShaderOrthoMatrix",       { ShaderOrthoMatrix() }),
        ShaderMeta("ShaderScaleMatrix",       { ShaderScaleMatrix() }),
        ShaderMeta("ShaderTranslateMatrix",   { ShaderTranslateMatrix() }),
        ShaderMeta("ShaderRotateMatrix",      { ShaderRotateMatrix() }),

        ShaderMeta("Texture3DShader",         { Texture3DShader() }),
        ShaderMeta("Texture3DClubShader",     { Texture3DClubShader() }),
        ShaderMeta("Texture3DMutiClubShader", { Texture3DMutiClubShader() }),
   )

    fun getShader(renderName: String): BaseShader {
        return findRenderMeta(renderName)?.shaderFactory?.invoke() ?: TriangleShader()
    }

    private fun findRenderMeta(renderName: String): ShaderMeta? {
        return shaderArray.find { it.renderName == renderName }
    }
}