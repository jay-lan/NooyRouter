package com.nooy.router.utils

import java.lang.StringBuilder

object PathUtils {
    fun formatPath(path: String): String {
        val pathNodes = path.split("/")
        val pathBuilder = StringBuilder()
        for (node in pathNodes) {
            if (node.trim().isNotEmpty()) {
                pathBuilder.append("/$node")
            }
        }
        if (pathBuilder.startsWith("/")) {
            return pathBuilder.substring(1).toString()
        } else {
            return pathBuilder.toString()
        }
    }
}