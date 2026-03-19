# OpenGL Programming

Android OpenGL ES 3.0 学习演示项目，展示了各种 OpenGL 渲染技术和效果。

## Shader 预创建与快速切换

> **重构说明**: 本章节架构由 [Claude Code](https://claude.ai/code) 设计实现，实现了 shader program 的预创建和快速切换能力。

## 功能特性

- **三角形渲染**: 基础三角形、VBO、VAO
- **纹理映射**: 2D 纹理、3D 立方体纹理
- **矩阵变换**: 正交投影、缩放、平移、旋转
- **YUV 渲染**: YUV 数据解析、亮度分离、颜色反转、分屏显示
- **Shader 练习**: Struct Array、多种着色器效果
- **Shader 预创建**: 快速切换，无需销毁重建

## 项目架构

```
┌─────────────────────────────────────────────────────────────────┐
│                          UI Layer                                │
│  ┌──────────────┐    ┌──────────────────────────────────────┐  │
│  │ MainActivity │───>│           FragmentDemo               │  │
│  └──────────────┘    │  ┌─────────────┐ ┌────────────────┐  │  │
│                      │  │   Button    │ │  PopupWindow   │  │  │
│                      │  │ (Shader选器)│ │ (下拉菜单列表) │  │  │
│                      │  └─────────────┘ └────────────────┘  │  │
│                      └──────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Render Layer                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     GlesSurfaceView                       │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │   │
│  │  │ GlesThread  │  │  EglHelper  │  │  SurfaceHolder  │  │   │
│  │  │ (GL渲染线程)│  │ (EGL环境管理)│  │  (Surface回调)  │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      BaseRender                           │   │
│  │  - onSurfaceCreated(): 初始化ShaderManager                │   │
│  │  - onDrawFrame(): 处理切换请求，绘制当前shader            │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      ShaderManager                               │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Map<String, BaseShader> shaders                         │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │   │
│  │  │Triangle │ │ Texture │ │   YUV   │ │ Matrix  │ ...    │   │
│  │  │ Shader  │ │ Shader  │ │ Shader  │ │ Shader  │        │   │
│  │  │Program=1│ │Program=2│ │Program=3│ │Program=4│        │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘        │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│              useShader(name) │                                   │
│                              ▼                                   │
│                       glUseProgram(id)                          │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Shader Layer                              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     BaseShader (抽象基类)                  │   │
│  │  - onInitGLES()      初始化OpenGL资源                      │   │
│  │  - onDestroyGLES()   销毁OpenGL资源                        │   │
│  │  - onActivate()      激活时调用（启动IO线程等）            │   │
│  │  - onDeactivate()    离开时调用（停止IO线程等）            │   │
│  │  - getRenderMode()   返回需要的渲染模式                    │   │
│  │  - onDrawFrame()     绘制帧                                │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                   │
│         ┌────────────────────┼────────────────────┐             │
│         ▼                    ▼                    ▼             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │TriangleShader│    │Texture2D    │    │ YUVRender   │         │
│  │    VBO/VAO  │    │   Shader    │    │  及其子类   │         │
│  └─────────────┘    └──────┬──────┘    └─────────────┘         │
│                            │                                    │
│                            ▼                                    │
│                    ┌─────────────┐                             │
│                    │ ShaderMatrix│                             │
│                    │   及其子类   │                             │
│                    │ (矩阵变换)   │                             │
│                    └─────────────┘                             │
└─────────────────────────────────────────────────────────────────┘
```


### 设计原理

OpenGL ES 的 Program 对象创建和链接开销较大（涉及 shader 编译），但切换开销极低（只需 `glUseProgram()`）。

因此在初始化时预创建所有 shader program，切换时只执行 `glUseProgram()`。

### 切换流程

```
旧流程（销毁重建）:
  switchShader() → reinitializeRenderer() → onSurfaceCreated()
  → glDeleteProgram() → glCreateProgram() → glLinkProgram()

新流程（预创建切换）:
  switchShader() → onDrawFrame() → glUseProgram()
```

### Shader 生命周期

```
初始化:
  onSurfaceCreated() → ShaderManager.initAllShaders()
    → 遍历所有shader: onSurfaceCreated() → onInitGLES()

切换:
  useShader(newName)
    → oldShader.onDeactivate()  // 停止IO线程等
    → resetGLState()            // 重置GL状态
    → glUseProgram(newProgram)
    → newShader.onSurfaceChanged()
    → setRenderMode()           // 切换渲染模式
    → newShader.onActivate()    // 启动IO线程等

销毁:
  destroyAll()
    → currentShader.onDeactivate()
    → 所有shader.destroyGLES()
```

### Render Mode 管理

| Shader 类型 | Render Mode | 说明 |
|-------------|-------------|------|
| Triangle/Texture/Matrix | CONTINUOUSLY | 连续渲染，适用于动画 |
| YUV 系列 | WHEN_DIRTY | 按需渲染，由IO线程触发 |

### GL 状态管理

切换 shader 时重置以下状态：
- `glClearColor(0, 0, 0, 1)` - 清除颜色

各 shader 自行管理的状态：
- `GL_DEPTH_TEST` - 3D shader 在 `onActivate()` 启用，`onDeactivate()` 禁用

## 目录结构

```
app/src/main/java/com/louiewh/opengl/
├── MainActivity.kt              # 主Activity
├── FragmentDemo.kt              # 演示Fragment，包含shader选择器
├── GlesSurfaceView.kt           # 自定义SurfaceView，管理GL线程
├── GlesConst.kt                 # Shader注册表
├── ShaderManager.kt             # Shader生命周期管理器（预创建、快速切换）
├── ContextUtil.kt               # Context工具类
├── GlslAssetsUtil.kt            # GLSL文件读取工具
│
├── render/
│   ├── BaseRender.kt            # 渲染器基类
│   └── GlesRender.kt            # 具体渲染器实现
│
└── shader/
    ├── BaseShader.kt            # Shader抽象基类
    ├── TriangleShader.kt        # 基础三角形
    ├── TriangleShaderVBO.kt     # VBO方式
    ├── TriangleShaderVAO.kt     # VAO方式
    ├── ShaderStructArray.kt     # Struct Array练习
    ├── Texture2DShader.kt       # 2D纹理
    ├── Texture3DShader.kt       # 3D纹理
    ├── Texture3DClubShader.kt   # 3D旋转立方体
    ├── Texture3DMutiClubShader.kt # 多个3D立方体
    ├── ShaderMatrix.kt          # 矩阵基类
    ├── ShaderOrthoMatrix.kt     # 正交投影矩阵
    ├── ShaderScaleMatrix.kt     # 缩放矩阵
    ├── ShaderTranslateMatrix.kt # 平移矩阵
    ├── ShaderRotateMatrix.kt    # 旋转矩阵
    ├── YUVRender.kt             # YUV渲染基类
    ├── YUVRenderLuma.kt         # 亮度分离
    ├── YUVRenderColorReverse.kt # 颜色反转
    ├── YUVRenderSplit2.kt       # 2分屏
    └── YUVRenderSplit4.kt       # 4分屏
```

## 如何添加新的 Shader

1. 在 `app/src/main/assets/glsl/` 中添加 `.vert` 和 `.frag` 文件

2. 创建新的 Shader 类继承 `BaseShader` 或其子类:

```kotlin
class MyShader : BaseShader() {
    override fun getVertexSource(): String = readGlslSource("MyShader.vert")
    override fun getFragmentSource(): String = readGlslSource("MyShader.frag")
    override fun onInitGLES(program: Int) { /* 初始化OpenGL资源 */ }
    override fun onDestroyGLES() { /* 销毁OpenGL资源 */ }
    override fun onDrawFrame(gl: GL10?) { /* 绘制逻辑 */ }

    // 可选：如果需要按需渲染（如YUV），重写此方法
    override fun getRenderMode(): Int = GLSurfaceView.RENDERMODE_WHEN_DIRTY

    // 可选：如果有后台线程等资源，重写这两个方法
    override fun onActivate() { /* 启动后台线程 */ }
    override fun onDeactivate() { /* 停止后台线程 */ }
}
```

3. 在 `GlesConst.kt` 中注册:

```kotlin
ShaderMeta("MyShader", { MyShader() }),
```

ShaderManager 会自动管理生命周期，无需其他修改。

## 技术要点

- **自定义 GlesSurfaceView**: 手动管理 EGL 上下文和 GL 线程
- **Shader 预创建**: 初始化时创建所有 program，切换时仅调用 glUseProgram()
- **生命周期管理**: onActivate/onDeactivate 管理后台线程等资源
- **Render Mode 切换**: 根据shader需求自动切换 CONTINUOUSLY/WHEN_DIRTY
- **YUV 渲染**: 演示 YUV420P 格式数据的解析和显示

## 运行要求

- Android SDK 24+
- OpenGL ES 3.0

## License

MIT