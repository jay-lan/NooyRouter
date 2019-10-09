package com.nooy.router.model

class RouteEventInfo(
    val eventName:String,
    val requestCode:Int,
    val className: String,
    val methodName:String,
    val paramTypes:Array<String>,
    val paramsKeys:Array<String>
)