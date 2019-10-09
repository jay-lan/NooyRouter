package com.nooy.router.annotation

/**
 * 路由数据，此注解修饰的属性可由框架进行自动赋值
 */
@Target(AnnotationTarget.FIELD)
annotation class RouteData(val key: String = "")