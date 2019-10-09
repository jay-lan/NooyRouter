package com.nooy.router.sample

import android.app.Application
import com.nooy.router.annotation.RouteApplication

@RouteApplication
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
    }
}