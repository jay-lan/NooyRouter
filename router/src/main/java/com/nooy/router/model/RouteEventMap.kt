package com.nooy.router.model

class RouteEventMap : HashMap<String, LinkedHashSet<RouteEventInfo>>() {
    fun putAll(list: List<RouteEventInfo>) {
        for (info in list) {
            put(info)
        }
    }

    fun put(vararg value: RouteEventInfo) {
        for (info in value) {
            val key = info.className
            val list = get(key)
            list.addAll(value)
            put(key, list)
        }
    }

    override fun get(key: String): LinkedHashSet<RouteEventInfo> {
        return super.get(key) ?: LinkedHashSet()
    }
}