package com.nooy.router.model

import com.nooy.router.utils.PathUtils

/**
 * 存储整个路由系统的路由表
 */
class RouteMap : HashMap<String, LinkedHashSet<RouteInfo>>() {
    fun putAll(list: List<RouteInfo>) {
        for (info in list) {
            put(info)
        }
    }

    fun put(vararg value: RouteInfo) {
        for (info in value) {
            val key = PathUtils.formatPath(info.path)
            val list = get(key)
            list.addAll(value)
            put(key, list)
        }
    }

    override fun get(key: String): LinkedHashSet<RouteInfo> {
        return super.get(key) ?: LinkedHashSet()
    }
}