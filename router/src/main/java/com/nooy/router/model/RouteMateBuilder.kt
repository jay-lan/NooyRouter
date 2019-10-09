package com.nooy.router.model

import com.nooy.router.core.RouteCore
import com.nooy.router.Router
import com.nooy.router.utils.RouteMateUtils
import com.orhanobut.logger.Logger

class RouteMateBuilder(val from: RouteMate = RouteMate.ROOT_ROUTE) {
    var routeMate = RouteMate.ROOT_ROUTE
    //构造路由
    fun to(path: String): RouteMateBuilder {
        val mapped = RouteCore.routeMap[path]
        var routeInfo: RouteInfo? = null
        if (mapped.size == 1) {
            routeInfo = mapped.first()
        } else if (mapped.size > 1) {
            //有多种匹配结果，调用多重匹配处理器选择后构造RouteMate
            routeInfo = Router.multiRouteMatchingHandler.onMultiRouteMatched(mapped)
        }
        if (routeInfo == null) {
            Logger.e("没有与路径[$path]匹配的内容")
            return this
        }
        routeMate = RouteMateUtils.buildRouteMate(from, routeInfo)
        return this
    }

}