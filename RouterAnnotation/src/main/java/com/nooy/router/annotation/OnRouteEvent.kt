package com.nooy.router.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class OnRouteEvent(val eventName: String = "", val requestCode: Int = 0)

annotation class Param(val name: String)