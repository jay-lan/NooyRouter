package com.nooy.router.utils

import com.nooy.router.*
import com.nooy.router.core.RouteCore
import com.nooy.router.model.RouteInfo
import com.nooy.router.model.RouteMate

object RouteMateUtils {

    fun getRouteMate(any: Any?): RouteMate? {
        any ?: return RouteMate.ROOT_ROUTE
        return RouteCore.getRouteMate(any)
    }

    fun buildRouteMate(source: RouteMate?, routeInfo: RouteInfo): RouteMate {
        return RouteMate(
            source?.routeId ?: 0,
            routeInfo.path,
            Router.getGroupName(routeInfo),
            null,
            Router.lastId++,
            routeInfo.routeType,
            routeInfo.routeMode,
            routeInfo.identifier
        )
    }
}