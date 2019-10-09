package com.nooy.router.model

import android.content.Context
import com.nooy.router.core.Navigator
import com.nooy.router.Router
import com.nooy.router.core.RouteCore

open class RouteConfigurator(val aimRoute: RouteMate) {

    var context: Context? = null

    open fun withFlags(flags: Int): RouteConfigurator {
        if (aimRoute == RouteMate.ROOT_ROUTE) return this
        aimRoute.flags = flags
        return this
    }

    open fun withData(key: String, data: Any): RouteConfigurator {
        if (aimRoute == RouteMate.ROOT_ROUTE) return this
        aimRoute.putData(key, data)
        return this
    }

    open fun withData(vararg keyValue: Pair<String, Any>): RouteConfigurator {
        if (aimRoute == RouteMate.ROOT_ROUTE) return this
        aimRoute.putData(*keyValue)
        return this
    }
    open fun withData(data: Any): RouteConfigurator = withData("default", data)

    open fun withData(dataMap: Map<String, Any>) = this.apply {
        if (aimRoute != RouteMate.ROOT_ROUTE) {
            aimRoute.putData(dataMap)
        }
    }

    open fun withTransition(animationIn: Int, animationOut: Int) {
        aimRoute.animationInId = animationIn
        aimRoute.animationOutId = animationOut
    }

    open fun withContext(context: Context): RouteConfigurator {
        this.context = context
        return this
    }

    open fun navigate(): Any? {
        if (aimRoute == RouteMate.ROOT_ROUTE) return null
        return Navigator.navigate(aimRoute,context ?: RouteCore.context)
    }

    companion object {
        val EMPTY_CONFIGURATOR = RouteConfigurator(RouteMate.ROOT_ROUTE)
    }
}