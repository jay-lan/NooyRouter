package com.nooy.router

import com.nooy.router.model.RouteInfo

interface MultiMatchingHandler {
    fun onMultiRouteMatched(infoList: Collection<RouteInfo>):RouteInfo
}

class SimpleMultiMatchingHandler:MultiMatchingHandler{
    override fun onMultiRouteMatched(infoList: Collection<RouteInfo>): RouteInfo {
        return infoList.last()
    }

}