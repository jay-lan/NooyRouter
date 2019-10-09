package com.nooy.router.model

import com.nooy.router.core.Navigator
import com.nooy.router.RouteModes
import com.nooy.router.RouteTypes

/**
 * 路由伴侣，每个路由目标都对应一个此实例，主要用于存储路由相关信息
 */
class RouteMate(
    /**
     * 来自哪里，路由ID
     */
    val from: Int,
    /**
     * 路由地址
     */
    val path: String,
    val group: String = "",
    /**
     * 路由目标的实例
     */
    val routeTarget: Any?,
    /**
     * 路由Id，由Router分配，用于区分不同的路由实例，在不同路由间通信中很重要，类似于IP地址
     */
    val routeId: Int,
    val routeType: RouteTypes,
    val routeMode: RouteModes,
    val identifier: String
) {
    /**
     * 存储路由数据
     */
    var dataMap = HashMap<String, Any>()
        set(value) {
            field.clear()
            field.putAll(value)
        }

    var flags = 0
    var animationInId: Int = 0
    var animationOutId: Int = 0

    /**
     * 装载数据
     */
    fun putData(vararg data: Pair<String, Any>) {
        dataMap.putAll(data)
    }

    fun putData(dataMap: Map<String, Any>) {
        this.dataMap.putAll(dataMap)
    }

    /**
     * 装载数据
     */
    fun putData(key: String, data: Any) {
        dataMap.put(key, data)
    }

    /**
     * 获取数据
     */
    fun <T> getData(key: String): T? {
        return dataMap[key] as T?
    }

    fun navigate() {
        Navigator.navigate(this)
    }

    companion object {
        val ROOT_ROUTE = RouteMate(-1, "", "", null, 0, RouteTypes.UNKNOWN, RouteModes.STANDARD, "")
    }
}