package com.louiewh.opengl

import android.content.Context

object ContextUtil {
    private lateinit var appContext: Context

    fun init(context: Context){
        appContext = context.applicationContext
    }

    fun getContext():Context{
        return appContext
    }
}
