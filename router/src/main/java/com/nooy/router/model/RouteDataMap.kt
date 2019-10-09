package com.nooy.router.model

import com.nooy.router.utils.PathUtils

/**
 * 路由数据映射表
 */
class RouteDataMap : HashMap<String, LinkedHashSet<RouteDataInfo>>() {
    fun putAll(list: List<RouteDataInfo>) {
        for (info in list) {
            put(info)
        }
    }

    fun put(vararg value: RouteDataInfo) {
        for (info in value) {
            val key = info.className
            val list = get(key)
            list.addAll(value)
            put(key, list)
        }
    }

    override fun get(key: String): LinkedHashSet<RouteDataInfo> {
        return super.get(key) ?: LinkedHashSet()
    }
}