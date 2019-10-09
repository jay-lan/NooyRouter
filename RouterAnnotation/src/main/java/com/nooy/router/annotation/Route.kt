package com.nooy.router.annotation

import com.nooy.router.RouteModes
import java.lang.annotation.Inherited

/**
 * 路由注解，能够作用于Activity、Fragment、View的子类和静态方法
 * 当作用于Activity时直接使用startActivity进行界面跳转
 * 作用于Fragment和View时返回对应的对象。
 */
annotation class Route(
    /**
     * 路由地址
     */
    val path: String,
    /**
     * 路由分组
     */
    val group:String = "",
    /**
     * 路由描述
     */
    val desc: String = "",
    /**
     * 路由模式，此配置仅对路由目标是Fragment和View的路由有效，包含四种路由模式（参照Activity的启动模式）：
     * 1、standard模式，每次路由时都会新建实例
     * 2、singleTop模式，如果路由的视图在栈顶则不会新建实例，只会调用路由目标中标记了@OnNewRoute注解的方法，方法参数可以用RouteData修饰，可传入对应值。
     * 3、singleTask模式，如果栈中不存在路由目标则新建实例，但若栈中已存在路由目标，则不管路由视图是否在栈顶均不会新建实例，只会将该实例放至栈顶，并
     *    调用路由目标中标记了@OnNewRoute注解的方法。
     * 4、singleInstance模式，全局只存在这一个路由目标，实例化后将会放至一个单独的栈，每次需要时取出
     */
    val routeMode: RouteModes = RouteModes.SINGLE_TASK
)
