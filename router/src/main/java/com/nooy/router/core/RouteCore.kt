package com.nooy.router.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.nooy.router.*
import com.nooy.router.lifecycle.RouterLifecycleObserver
import com.nooy.router.model.*
import com.nooy.router.utils.ReflectUtils
import com.nooy.router.utils.RouteInfoUtils
import com.nooy.router.view.RouteView
import com.orhanobut.logger.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet

object RouteCore {
    //val lifecycleObserverMap = HashMap<LifecycleOwner, LifecycleObserver>()
    //存放路由伴侣
    //存放未绑定目标的路由伴侣，通常是activity或service，因为activity和service由系统创建，无法立即绑定
    //存放数据映射路径，方便路由传输时进行数据绑定
    var application: Application? = null
    val context: Context
        get() {
            return currentActivity ?: (application
                ?: throw IllegalStateException("you must call Router.init in your Application"))
        }
    var currentActivity: Activity? = null
    val routeDataMap = RouteDataMap()
    val routeViewMap = RouteDataMap()
    val routeEventMap = RouteEventMap()
    val routeMap = RouteMap()
    //对象池，存有在路由器中注册的所有对象，key为class，value为对象的集合，这里为了不影响垃圾回收，只保留对象的弱引用
    private val objectPool = HashMap<String, HashSet<WeakReference<Any>>>()

    private val referenceQueue = ReferenceQueue<Any>()

    //事件对象表
    private val eventObjectMap = HashMap<String, LinkedHashSet<WeakReference<Any>>>()

    private val onRouteInfoLoadedListeners = HashSet<(routeInfos: List<RouteInfo>) -> Unit>()

    private val handler = Handler()

    var classLoader: ClassLoader = javaClass.classLoader!!

    fun putIntoObjectPool(any: Any) {
        val clazz = any.javaClass
        val key = clazz.canonicalName ?: ""
        val objectList = objectPool[key] ?: HashSet()
        objectList.add(WeakReference(any, referenceQueue))
        objectPool[key] = objectList
    }

    /**
     * 将对象和路由伴侣绑定起来，方便通过对象获取路由伴侣以及获得路由的相关信息
     */
    fun bindRouteMate(any: Any) {
        /*if (!routeMateMap.containsKey(any)) {
            val className = any.javaClass.canonicalName ?: return
            routeMateMap[any] = routeMateMapUnbound[className] ?: return
            routeMateMapUnbound.remove(className)
        }*/
    }

    fun getRouteMate(any: Any): RouteMate? {
        return null
    }

    fun removeRouteMate(any: Any) {
        /*if (routeMateMap.containsKey(any)) {
            routeMateMap.remove(any)
        }*/
    }

    fun putUnboundRouteMate(key: String, routeMate: RouteMate) {
        //routeMateMapUnbound[key] = routeMate
    }

    fun injectData(any: Any) {
        val routeMate = getRouteMate(any)
        routeMate ?: return
        if (routeMate.dataMap.isEmpty()) return
        val className = any.javaClass.canonicalName ?: ""
        val dataList = routeDataMap[className]
        val clazz = Class.forName(className)
        for (dataInfo in dataList) {
            try {
                val field = clazz.getDeclaredField(dataInfo.fieldName)
                field.isAccessible = true
                field.set(any, routeMate.getData(dataInfo.key))
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
        routeMate.dataMap.clear()
    }
    fun init(application: Application) {
        this.application = application
        val packageName = application.javaClass.`package`?.name ?: ""
        loadPackage("$packageName.router")
    }
    fun loadPackage(packageName: String, classLoader: ClassLoader = this.classLoader) {
        //初始化路由信息映射表
        loadRouteInfoListFromPackage(packageName, classLoader)
        //初始化路由数据映射表
        loadRouteDataInfoListFromPackage(packageName, classLoader)
        //初始化RouteView表
        loadRouteViewDataInfoListFromPackage(packageName, classLoader)
        //初始化RouteEvent
        loadRouteEventInfoListFromPackage(packageName, classLoader)
    }

    private fun loadRouteInfoListFromPackage(packageName: String, classLoader: ClassLoader = this.classLoader) {
        try {
            val clazz = classLoader.loadClass("$packageName.$CLASS_NAME_ROUTE_MAP")
            val routeInfoList = clazz.getMethod(METHOD_NAME_GET_ROUTE_INFO_LIST).invoke(null) as List<RouteInfo>
            routeInfoList.filter { it.routeType == RouteTypes.UNKNOWN }.forEach { RouteInfoUtils.rebuildRouteInfo(it) }
            routeMap.putAll(routeInfoList)
            emitRouteInfoLoadEvent(routeInfoList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRouteDataInfoListFromPackage(packageName: String, classLoader: ClassLoader = this.classLoader) {
        try {
            val clazz = classLoader.loadClass("$packageName.$CLASS_NAME_ROUTE_DATA_MAP")
            val routeInfoList = clazz.getMethod(METHOD_NAME_GET_DATA_INFO_LIST).invoke(null) as List<RouteDataInfo>
            routeDataMap.putAll(routeInfoList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRouteViewDataInfoListFromPackage(packageName: String, classLoader: ClassLoader = this.classLoader) {
        try {
            val clazz = classLoader.loadClass("$packageName.$CLASS_NAME_ROUTE_VIEW_MAP")
            val routeInfoList = clazz.getMethod(METHOD_NAME_GET_DATA_INFO_LIST).invoke(null) as List<RouteDataInfo>
            routeViewMap.putAll(routeInfoList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadRouteEventInfoListFromPackage(packageName: String, classLoader: ClassLoader = this.classLoader) {
        try {
            val clazz = classLoader.loadClass("$packageName.$CLASS_NAME_ROUTE_EVENT_MAP")
            val routeEventList = clazz.getMethod(METHOD_NAME_GET_ROUTE_EVENT_LIST).invoke(null) as List<RouteEventInfo>
            routeEventMap.putAll(routeEventList)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun register(obj: Any) {
        putIntoObjectPool(obj)
        val lifecycleOwner: LifecycleOwner? =
            if (obj is LifecycleOwner) obj else if (obj is View && obj.context is LifecycleOwner) obj.context as LifecycleOwner else null
        if (lifecycleOwner!=null) {
            Router.bindLifecycle(lifecycleOwner, obj)
        }
        if (obj is Activity && !obj.isDestroyed) {
            currentActivity = obj
            DataInjector.injectActivity(obj)
        }
        bindEvents(obj)
    }
    fun injectData(any: Any, dataMap: Map<String, Any?>) {
        val className = any.javaClass.canonicalName ?: ""
        val dataList = routeDataMap[className]
        val clazz = Class.forName(className)
        for (dataInfo in dataList) {
            try {
                val field = clazz.getDeclaredField(dataInfo.fieldName)
                field.isAccessible = true
                field.set(any, dataMap[dataInfo.key])
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
    }

    fun bindDataTo(aim: Any, key: String, data: Any) {
        val clazz = aim.javaClass
        val field = clazz.getDeclaredField(key)
        field.isAccessible = true
        field.set(aim, data)
    }

    fun injectRouteView(any: Any, routeView: RouteView) {
        val className = any.javaClass.canonicalName ?: ""
        val dataList = routeViewMap[className]
        try {
            val clazz = Class.forName(className)
            for (dataInfo in dataList) {
                val field = try {
                    clazz.getField(dataInfo.fieldName)
                } catch (e: Exception) {
                    continue
                }
                field.isAccessible = true
                field.set(any, routeView)
            }
        } catch (e: Exception) {
            if (Router.isDebug) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 将对象绑定到一个lifecycleOwner，当这个lifecycleOwner被销毁时，该对象也会被销毁
     */
    fun bindLifecycle(lifecycleOwner: LifecycleOwner, obj: Any) {
        val lifecycleObserver = RouterLifecycleObserver(obj, lifecycleOwner)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        //RouteCore.lifecycleObserverMap[obj] = lifecycleObserver
    }

    /**
     * 将对象中注册的事件注册到事件列表中
     */
    fun bindEvents(any: Any) {
        val clazzName = any::class.java.canonicalName!!
        val events = routeEventMap[clazzName]
        for (event in events) {
            val key = getEventKey(event)
            val objList = eventObjectMap[key] ?: LinkedHashSet()
            objList.add(WeakReference(any))
            eventObjectMap[key] = objList
        }
    }

    fun getEventKey(eventInfo: RouteEventInfo): String {
        return "${eventInfo.eventName}${eventInfo.requestCode}"
    }

    fun destroyAllView() {
        /*val allViews: List<View> = routeMateMap.keys.filter { it is View } as List<View>
        allViews.forEach {
            *//*routeMateMap.remove(it)
            val events = routeEventMap[it::class.java.canonicalName ?: ""]
            for (event in events) {
                val key = getEventKey(event)
                eventObjectMap[key]?.remove(it)
            }*//*
        }*/
    }

    fun destroyObject(any: Any?, dispatchEvent: Boolean = true) {
        return
        any ?: return
        //移除对象与RouteMate映射表中的数据
        /*val mate = routeMateMap[any]
        if (mate != null) {
            routeMateMap.remove(any)
            if (routeMateMapUnbound.containsValue(mate)) {
                routeMateMapUnbound.remove(any::class.java.canonicalName ?: "")
            }
        }

        //将对象从对象池中移除
        val objectList = objectPool[any.javaClass.canonicalName ?: ""] ?: emptySet<WeakReference<Any>>()
        if (any in objectList) {
            //还没销毁
            objectPool[any.javaClass.canonicalName ?: ""]?.remove(any)
            //分发对象销毁事件
            if (dispatchEvent) {
                dispatchEventTo(RouteEvents.ON_DESTROY, any, HashMap())
            }
            //移除事件监听表中数据
            val events = routeEventMap[any::class.java.canonicalName ?: ""]
            for (event in events) {
                val key = getEventKey(event)
                eventObjectMap[key]?.remove(any)
            }
        }*/
    }

    /**
     * 分发事件
     */
    fun dispatchEvent(eventName: String, requestCode: Int = 0, dataMap: Map<String, Any> = HashMap()) =
        GlobalScope.async {
            val key = "$eventName$requestCode"
            val referenceList = eventObjectMap[key] ?: LinkedHashSet()
            val removeReferenceList = ArrayList<WeakReference<Any>>()
            for (reference in referenceList) {
                val obj = reference.get()
                if (obj == null) {
                    //对象已被回收，移除该引用
                    removeReferenceList.add(reference)
                } else {
                    //对象还未被回收
                    dispatchEventTo(eventName, requestCode, obj, dataMap)
                }
            }
            referenceList.removeAll(removeReferenceList)
        }

    fun loadValue(clazz: Class<*>, fieldName: String): Any? {
        val referenceList = objectPool[clazz.canonicalName ?: ""] ?: return null
        if (referenceList.isEmpty()) return null
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return if (referenceList.size == 1) {
            val reference = referenceList.first()
            val obj = reference.get()
            if (obj == null) {
                //说明这个对象已经被回收了
            }
            field.get(referenceList.first().get() ?: return null)
        } else {
            val resultArray = Array<Any?>(referenceList.size) { null }
            var curPos = 0
            referenceList.forEach {
                resultArray[curPos++] = field.get(it.get())
            }
            resultArray
        }
    }

    fun callMethod(clazz: Class<*>, method: Method, vararg params: Any?): Any? {
        val referenceList = objectPool[clazz.canonicalName ?: ""] ?: return null
        if (referenceList.isEmpty()) return null
        return if (referenceList.size == 1) {
            method.invoke(referenceList.first().get() ?: return null, *params)
        } else {
            val resultArray = Array<Any?>(referenceList.size) { null }
            var curPos = 0
            referenceList.forEach {
                resultArray[curPos++] = method.invoke(it, *params)
            }
            resultArray
        }
    }

    fun dispatchEventTo(eventName: String, requestCode: Int, obj: Any, dataMap: Map<String, Any>) = GlobalScope.async {
        //执行事件
        val eventInfoList = routeEventMap[obj::class.java.canonicalName ?: ""] ?: LinkedHashSet()
        val eventInfo =
            eventInfoList.find { it.eventName == eventName && it.requestCode == requestCode }
        if (eventInfo != null) {
            val arrCLazz = arrayOf(0)::class.java.canonicalName
            val clazz = obj::class.java
            val methods = clazz.declaredMethods
            val method = clazz.getDeclaredMethod(
                eventInfo.methodName,
                *eventInfo.paramTypes.map { ReflectUtils.fromClassName(it) }.toTypedArray()
            )
            //构造参数表
            val params = if (eventInfo.paramTypes.size == 1 && dataMap.contains("default")) {
                arrayOf(dataMap["default"])
            } else {
                eventInfo.paramsKeys.map { dataMap[it] }.toTypedArray()
            }
            try {
                handler.post {
                    method.invoke(obj, *params)
                }
            } catch (e: Exception) {
                throw IllegalArgumentException("传递的参数与路由的方法参数不匹配", e.cause)
            }
        }
    }

    fun dispatchEventTo(eventName: String, obj: Any, dataMap: Map<String, Any> = HashMap()) =
        dispatchEventTo(eventName, 0, obj, dataMap)

    fun dispatchEvent(eventName: String, dataMap: Map<String, Any>) =
        dispatchEvent(eventName, 0, dataMap)

    fun addOnRouteInfoLoadedListener(listener: (info: List<RouteInfo>) -> Unit) {
        onRouteInfoLoadedListeners.add(listener)
    }

    fun removeOnRouteInfoLoadedListener(listener: (info: List<RouteInfo>) -> Unit) {
        onRouteInfoLoadedListeners.remove(listener)
    }

    fun emitRouteInfoLoadEvent(info: List<RouteInfo>) {
        onRouteInfoLoadedListeners.forEach {
            it(info)
        }
    }

    fun checkReference(referenceCollection: MutableCollection<WeakReference<*>>, reference: WeakReference<*>): Any? {
        val obj = reference.get()
        if (obj == null) {
            Logger.d("对象被回收了，移除弱引用")
            referenceCollection.remove(reference)
        }
        return obj
    }

}