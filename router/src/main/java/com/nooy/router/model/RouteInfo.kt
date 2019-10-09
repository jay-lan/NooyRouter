package com.nooy.router.model

import com.nooy.router.RouteModes
import com.nooy.router.RouteTypes

class RouteInfo(
    val path: String,
    val group: String,
    var routeType: RouteTypes,
    /**
     * 标识符，如果路由目标是类就是完整类名，是方法和属性就是 完整类名:方法名/属性名
     */
    val identifier: String,
    val routeMode: RouteModes,
    val desc: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RouteInfo

        if (identifier != other.identifier) return false

        return true
    }

    override fun hashCode(): Int {
        return identifier.hashCode()
    }
}