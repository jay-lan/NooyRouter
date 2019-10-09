package com.nooy.router.sample

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.nooy.router.annotation.OnRouteEvent
import com.nooy.router.constants.RouteEvents

class MyView:View {
    constructor(context: Context?) : super(context){
        println("Context")
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, int: Int) : this(context){
        println("Context,Int")
    }

    init {
        println("init")
    }
}