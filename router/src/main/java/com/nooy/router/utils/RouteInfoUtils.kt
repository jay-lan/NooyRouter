package com.nooy.router.utils

import android.app.Activity
import androidx.fragment.app.Fragment
import android.view.View
import com.nooy.router.model.RouteInfo
import com.nooy.router.RouteTypes
import java.lang.Exception

object RouteInfoUtils {
    fun rebuildRouteInfo(routeInfo: RouteInfo) {
        if (routeInfo.routeType == RouteTypes.UNKNOWN) {
            val identifier = routeInfo.identifier
            try {
                val clazz = Class.forName(identifier)
                if (ReflectUtils.isChildClassOf(clazz, Activity::class.java)) {
                    routeInfo.routeType = RouteTypes.ACTIVITY
                }else if (ReflectUtils.isChildClassOf(clazz, androidx.fragment.app.Fragment::class.java)) {
                    routeInfo.routeType = RouteTypes.FRAGMENT
                }else if (ReflectUtils.isChildClassOf(clazz, View::class.java)) {
                    routeInfo.routeType = RouteTypes.VIEW
                }
            } catch (e: Exception) {

            }
        }
    }
}