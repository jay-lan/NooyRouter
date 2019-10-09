package com.nooy.router.annotation

@Target(AnnotationTarget.CLASS)
annotation class DataInjector(val priority:Int = -1)