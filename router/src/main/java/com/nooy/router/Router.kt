package com.nooy.router

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.view.View
import com.nooy.router.core.RouteCore
import com.nooy.router.model.*
import com.nooy.router.utils.PathUtils
import com.nooy.router.utils.RouteInfoUtils
import com.nooy.router.utils.RouteMateUtils
import com.orhanobut.logger.Logger
import java.lang.Exception

/**
 * 这是一个单例，用户可在任何地方调用这个单例的方法进行路由
 */
@SuppressLint("StaticFieldLeak")
object Router {
    var isDebug: Boolean = true
    var multiRouteMatchingHandler: MultiMatchingHandler = SimpleMultiMatchingHandler()
    var lastId = 0
    fun dispatchEvent(eventName: String, requestCode: Int = 0, data: Any? = null) {
        RouteCore.dispatchEvent(
            eventName,
            requestCode,
            if (data == null) HashMap() else if (data is Map<*, *>) data as Map<String, Any> else mapOf("default" to data)
        )
    }

    fun to(path: String, from: Any? = null): RouteConfigurator {
        val routeMate = mapAndBuildRouteMate(path, from) ?: return RouteConfigurator.EMPTY_CONFIGURATOR
        return RouteConfigurator(routeMate)
    }

    fun mapAndBuildRouteMate(path: String, from: Any?): RouteMate? {
        val routeInfo = map(path) ?: return null
        return RouteMateUtils.buildRouteMate(RouteMateUtils.getRouteMate(from), routeInfo)
    }

    fun map(path: String): RouteInfo? {
        val mapped = RouteCore.routeMap[path]
        var routeInfo: RouteInfo? = null
        if (mapped.size == 1) {
            routeInfo = mapped.first()
        } else if (mapped.size > 1) {
            //有多种匹配结果，调用多重匹配处理器选择后构造RouteMate
            routeInfo = Router.multiRouteMatchingHandler.onMultiRouteMatched(mapped)
        }
        return routeInfo
    }

    fun getRouteTarget(routeInfo: RouteInfo): Any? {
        try {
            when (routeInfo.routeType) {
                RouteTypes.VIEW -> {
                    if (RouteCore.currentActivity == null) {
                        if (isDebug) {
                            Logger.d("Before creating a View,please invoke the function 'bind' to bind a activity first")
                        }
                    } else {
                        return null
                    }
                }
                RouteTypes.ACTIVITY -> {
                    return Class.forName(routeInfo.identifier)
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            if (isDebug) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * 将对象绑定到一个lifecycleOwner，当这个lifecycleOwner被销毁时，该对象也会被销毁
     */
    fun bindLifecycle(lifecycleOwner: LifecycleOwner, obj: Any) {
        RouteCore.bindLifecycle(lifecycleOwner, obj)
    }

    fun getGroupName(route: RouteInfo): String {
        return if (route.group.trim() === "") {
            //开发者没有指定分组，此时根据path获取分组
            val path = PathUtils.formatPath(route.path)
            val pos = path.indexOf("/")
            if (pos == -1) {
                "root"
            } else {
                path.substring(0, pos)
            }
        } else {
            route.group
        }
    }
}