package com.nooy.router.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.nooy.router.RouteTypes
import com.nooy.router.Router
import com.nooy.router.constants.RouteEvents
import com.nooy.router.model.RouteMate
import com.nooy.router.utils.ReflectUtils
import com.orhanobut.logger.Logger
import java.lang.Exception
import java.lang.reflect.Method

/**
 * 导航器
 */
object Navigator {
    fun navigate(target: RouteMate, context: Context? = null): Any? {
        val context = context ?: RouteCore.context
        when (target.routeType) {
            RouteTypes.ACTIVITY -> {
                //TODO:本来是通过这个方法来传递参数的，但是在高并发的情况下会出问题，比如同时启动10个相同的activity，
                // 只有最后一个activity会获取到数据，因此应该使用Intent来传递数据
                //RouteCore.putUnboundRouteMate(target.identifier, target)
                val intent = Intent(context, ReflectUtils.fromClassName(target.identifier))
                DataInjector.writeData2Intent(intent,target)
                if (context !is Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    Logger.w("从非Activity的Context启动Activity")
                }
                context.startActivity(intent)
            }
            RouteTypes.METHOD, RouteTypes.FIELD -> {
                val identifier = target.identifier

                val strs = identifier.split(":")
                if (strs.size != 2) {
                    Logger.e("方法或属性标识符异常：$identifier")
                    return null
                }
                val className = strs[0]
                val name = strs[1]
                val clazz = ReflectUtils.fromClassName(className)
                if (target.routeType == RouteTypes.FIELD) {
                    //判断属性是否是静态属性，是静态属性直接从类中取值，如果不是静态属性，需要在已注册的对象中取值
                    //优先取静态值，如果不是静态值则从已注册对象中取值
                    return ReflectUtils.getStaticValue(clazz, clazz.getDeclaredField(name))
                        ?: RouteCore.loadValue(clazz, name)
                } else {
                    //是个方法
                    val methodSplit = name.split("|")
                    lateinit var method: Method
                    var params = emptyArray<Any?>()
                    if (methodSplit.size == 2) {
                        //有方法参数，获取参数类型
                        val paramTypeNames = methodSplit[1].split(",")
                        val paramTypes =
                            Array(paramTypeNames.size) {
                                //根据参数类型获取参数类，以便从类中找对应的方法
                                //方法参数信息的存储方式是[参数名@参数类型]，因此@后面的才是参数类型
                                ReflectUtils.fromClassName(paramTypeNames[it].split("@")[1])
                            }
                        if (paramTypeNames.isEmpty()) {
                            //如果没有参数，直接执行
                            method = clazz.getDeclaredMethod(methodSplit[0])
                        } else {
                            //有参数，从对应的路由伴侣中取出传递的数据
                            //当参数只有一个时，先从路由伴侣中根据名称加载数据，如果没有对应名称的数据，则看是否存在名为default的数据，
                            //如果存在则将这个数据取出来
                            params =
                                Array(paramTypeNames.size) {
                                    target.getData<Any>(paramTypeNames[it].split("@")[0])
                                        ?: if (paramTypeNames.size == 1) target.getData<Any>("default") else null
                                }
                            method = clazz.getMethod(methodSplit[0], *paramTypes)
                        }
                    } else {
                        method = clazz.getMethod(name)
                    }
                    //先寻找静态方法执行，当没有静态方法时再从已注册的对象中执行方法
                    return try {
                        ReflectUtils.invokeStaticMethod(clazz, method, *params)
                            ?: RouteCore.callMethod(clazz, method, *params)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            RouteTypes.VIEW -> {
                val viewClass = ReflectUtils.fromClassName(target.identifier)
                val instance: View = try {
                    viewClass.getConstructor(Context::class.java)
                        .newInstance(context) as View
                } catch (e: Exception) {
                    e.printStackTrace()
                    return null
                }
                //将对象放入routeMateMap中，以便可以通过对象取出该对象对应的RouteMate
                //RouteCore.routeMateMap[instance] = target
                //为View绑定生命周期
                if (context is LifecycleOwner) {
                    RouteCore.bindLifecycle(context, instance)
                }

                //绑定声明周期，以便在承载这个View的activity销毁时同时销毁掉这个View
                //注入数据
                RouteCore.injectData(instance, target.dataMap)
                //事件绑定
                RouteCore.bindEvents(instance)
                //给RouteMate的RouteTarget赋值
                ReflectUtils.setFieldValue(target, "routeTarget", instance)
                //分发事件OnCreated
                RouteCore.dispatchEventTo(RouteEvents.ON_CREATED, instance, mapOf("default" to target))
                return instance
            }
            RouteTypes.SERVICE -> TODO()
            RouteTypes.FRAGMENT -> TODO()
            RouteTypes.UNKNOWN -> TODO()
        }
        return null
    }
}