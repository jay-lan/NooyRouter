package com.nooy.router.view

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import com.nooy.router.*
import com.nooy.router.constants.EventParams
import com.nooy.router.constants.RouteEvents
import com.nooy.router.core.Navigator
import com.nooy.router.core.RouteCore
import com.nooy.router.model.RouteConfigurator
import com.nooy.router.model.RouteMate
import com.nooy.router.utils.ReflectUtils
import com.nooy.router.utils.RouteMateUtils
import java.lang.Exception
import java.util.*
import kotlin.collections.HashMap


class RouteView : FrameLayout, LifecycleObserver {

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val array = context.obtainStyledAttributes(attrs, R.styleable.RouteView)
        animationDuration = array.getInt(R.styleable.RouteView_anim_duration, 200)
        val inAnimationName = array.getString(R.styleable.RouteView_in_animation)
        var inAnimationId = 0
        if (inAnimationName != null) {
            inAnimationId = context.resources.getIdentifier(
                inAnimationName.substring(
                    inAnimationName.lastIndexOf("/") + 1,
                    inAnimationName.indexOf(".")
                ), "anim", context.packageName
            )
        }
        inAnimation = if (inAnimationId == 0) {
            AnimationUtils.loadAnimation(context, R.anim.translate_right_in)
        } else {
            AnimationUtils.loadAnimation(context, inAnimationId)
        }
        val outAnimationName = array.getString(R.styleable.RouteView_out_animation)
        var outAnimationId = 0
        if (outAnimationName != null) {
            outAnimationId = resources.getIdentifier(
                outAnimationName.substring(
                    outAnimationName.lastIndexOf("/") + 1,
                    outAnimationName.indexOf(".")
                ), "anim", context.packageName
            )
        }
        outAnimation = if (outAnimationId == 0) {
            AnimationUtils.loadAnimation(context, R.anim.translate_right_out)
        } else {
            AnimationUtils.loadAnimation(context, outAnimationId)
        }
        val path = array.getString(R.styleable.RouteView_path)
        val showAnimation = array.getBoolean(R.styleable.RouteView_show_animation, false)
        val setupWithAnimation = array.getBoolean(R.styleable.RouteView_setup_with_animation, false)
        if (setupWithAnimation) {
            this.showTranslateAnimation = true
        }
        if (path != null) {
            to(path).navigate()
        }
        this.showTranslateAnimation = showAnimation
        array.recycle()
    }

    /**
     * 该路由视图的视图栈
     */
    private val viewStack = Stack<RouteMate>()

    private val singleTaskMap = HashMap<String, RouteMate>()

    private var isAnimationShowing = false

    var lifecycle: Lifecycle? = null
    var animationDuration = 200
    var inAnimation: Animation = AnimationUtils.loadAnimation(context, R.anim.translate_right_in)
        set(value) {
            field = value
            if (value.duration == 0L) {
                value.duration = animationDuration.toLong()
            }
        }
    var outAnimation: Animation = AnimationUtils.loadAnimation(context, R.anim.translate_right_out)
        set(value) {
            field = value
            if (value.duration == 0L) {
                value.duration = animationDuration.toLong()
            }
        }
    var showTranslateAnimation = false
    var requestCode: Int = Random().nextInt()

    init {
        if (context is LifecycleOwner) {
            bindLifecycle(context as LifecycleOwner)
        }
    }

    fun bindLifecycle(lifecycleOwner: LifecycleOwner) {
        this.lifecycle = lifecycleOwner.lifecycle
        lifecycleOwner.lifecycle.addObserver(this)
    }

    fun unbindLifecycle() {
        lifecycle?.removeObserver(this)
    }

    override fun addView(child: View?) {
        if (isAnimationShowing) {
            super.addView(child)
        } else {
            if (childCount > 0) {
                throw Exception("The view group can only have one child.")
            } else {
                super.addView(child)
            }
        }
    }

    override fun removeAllViews() {
        //清空所有视图
        for (viewIndex in viewStack.indices) {
            dispatchEvent(RouteEvents.ON_HIDE, index = viewIndex)
            dispatchEvent(RouteEvents.ON_DESTROY, index = viewIndex)
            destroyView(viewStack[viewIndex].routeTarget as View)
        }
        viewStack.clear()
        singleTaskMap.clear()
        if (!showTranslateAnimation) {
            super.removeAllViews()
        } else if (childCount > 0) {
            val child = getChildAt(0)
            outAnimation.setAnimationListener(OutAnimationListener(child))
            child.startAnimation(outAnimation)
        }
    }

    fun to(path: String): RouteConfigurator {
        val routInfo = Router.map(path) ?: return RouteConfigurator.EMPTY_CONFIGURATOR
        val routeMate = RouteMateUtils.buildRouteMate(
            RouteCore.getRouteMate(if (viewStack.empty()) this else viewStack.peek()),
            routInfo
        )
        return object : RouteConfigurator(routeMate) {
            override fun navigate(): Any? {
                return if (routeMate.routeType == RouteTypes.VIEW || routeMate.routeType == RouteTypes.FRAGMENT) {
                    val result = navigateTo(routeMate)
                    setContentView(result as View)
                    result
                } else {
                    Navigator.navigate(routeMate)
                }
            }
        }
    }

    private fun navigateTo(routeMate: RouteMate): Any? {
        return when (routeMate.routeMode) {
            RouteModes.STANDARD -> {
                createNewViewInstance(routeMate)
            }
            RouteModes.SINGLE_TOP -> {
                if (viewStack.peek().path == routeMate.path) {
                    //栈顶是路由目标
                    val originAim = viewStack.peek()
                    //更新路由数据
                    originAim.dataMap = routeMate.dataMap
                    RouteCore.injectData(originAim)
                    //发送事件通知View
                    val totalDataMap = HashMap<String, Any>()
                    totalDataMap[EventParams.DATA_MAP] = originAim.dataMap
                    totalDataMap.putAll(originAim.dataMap)
                    dispatchEvent(RouteEvents.ON_NEW_ROUTE, dataMap = totalDataMap)
                    originAim.routeTarget
                } else {
                    //目标不在栈顶，因此需要新建实例
                    createNewViewInstance(routeMate)
                }
            }
            RouteModes.SINGLE_TASK -> {
                //单任务模式，一个RouterView中，只有一个实例
                if (singleTaskMap.containsKey(routeMate.path)) {
                    //View栈中有此视图，此时需要将视图提至栈顶，并调用目标的OnNewRoute方法通知目标
                    val originMate = singleTaskMap[routeMate.path] ?: routeMate
                    if (originMate.routeTarget == null) {
                        val target = Navigator.navigate(routeMate) ?: throw Exception("创建路由目标失败")
                        ReflectUtils.setFieldValue(originMate, "routeTarget", target)
                    }
                    originMate.dataMap = routeMate.dataMap
                    RouteCore.injectData(originMate.routeTarget!!)
                    //发送事件通知View
                    val totalDataMap = HashMap<String, Any>()
                    totalDataMap[EventParams.DATA_MAP] = originMate.dataMap
                    totalDataMap.putAll(originMate.dataMap)
                    val index = viewStack.indexOf(originMate)
                    dispatchEvent(
                        RouteEvents.ON_NEW_ROUTE,
                        index = index,
                        dataMap = totalDataMap
                    )
                    //将视图移至栈顶
                    move2Top(originMate)
                    originMate.routeTarget
                } else {
                    //新建路由实例
                    singleTaskMap[routeMate.path] = routeMate
                    createNewViewInstance(routeMate)
                }
            }
            RouteModes.SINGLE_INSTANCE -> {
                TODO("实现SingleInstance启动模式")
            }
        }
    }

    fun dispatchEventToChildren(eventName: String, requestCode: Int, dataMap: Map<String, Any> = HashMap()) {

    }

    /**
     * 分发事件，只能分发声明周期事件
     */
    fun dispatchEvent(
        eventName: String,
        requestCode: Int = 0,
        index: Int = viewStack.lastIndex,
        dataMap: Map<String, Any> = HashMap()
    ) {
        Router.dispatchEvent(eventName, this.requestCode, dataMap)
        //遍历子视图
        if (index in viewStack.indices) {
            val routeMate = viewStack[index]
            RouteCore.dispatchEventTo(eventName, requestCode, routeMate.routeTarget!!, dataMap)
        }
    }

    fun dispatchEvent2Children(eventName: String, requestCode: Int = 0, dataMap: Map<String, Any> = EMPTY_MAP) {
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            dispatchEvent(eventName, child, requestCode, dataMap)
        }
    }

    fun dispatchEvent(
        eventName: String,
        view: View,
        requestCode: Int = 0,
        dataMap: Map<String, Any> = EMPTY_MAP
    ) {
        if (view is ViewGroup) {
            val childCount = view.childCount
            for (i in 0 until childCount) {
                dispatchEvent(eventName, view.getChildAt(i), requestCode, dataMap)
            }
        }
        RouteCore.dispatchEventTo(eventName, requestCode, view, dataMap)
    }

    fun back() {
        //退栈
        val mate = viewStack.pop()
        singleTaskMap.remove(mate.path)
        setContentView(viewStack.peek().routeTarget as View)
        destroyView(mate.routeTarget as View)
    }

    fun canBack() = viewStack.size > 1

    private fun createNewViewInstance(routeMate: RouteMate): Any {
        val view = Navigator.navigate(routeMate) ?: throw Exception("创建路由目标失败")
        RouteCore.injectRouteView(view, this)
        viewStack.push(routeMate)
        return view
    }

    private fun move2Top(mate: RouteMate) {
        if (viewStack.peek() == mate) return
        viewStack.remove(mate)
        viewStack.push(mate)
    }

    fun setContentView(view: View) {
        val originView = if (childCount > 0) getChildAt(0) else null
        if (originView == view) return
        if (!showTranslateAnimation) {
            if (originView != null) {
                dispatchEvent2Children(RouteEvents.ON_HIDE)
                Router.dispatchEvent(RouteEvents.ON_HIDE, this.requestCode, view)
            }
            removeView(originView)
            addView(view)
            view.post {
                dispatchEvent2Children(RouteEvents.ON_SHOW)
                Router.dispatchEvent(RouteEvents.ON_SHOW, this.requestCode, view)
            }

            return
        }
        //有过度动画
        outAnimation.setAnimationListener(OutAnimationListener(originView))
        if (originView == null) {
            post {
                removeView(originView)
            }
        } else {
            originView.startAnimation(outAnimation)
        }
        isAnimationShowing = true
        addView(view)
        view.startAnimation(inAnimation)
        dispatchEvent(RouteEvents.ON_SHOW,view)
        RouteCore.dispatchEvent(RouteEvents.ON_SHOW, this.requestCode, HashMap())
    }

    fun destroyAllView() {
        for (mate in viewStack) {
            destroyView(mate.routeTarget as View)
        }
    }


    fun destroyView(view: View) {
        //先摧毁子视图
        if (view is RouteView) {
            view.unbindLifecycle()
            view.destroyAllView()
        } else if (view is ViewGroup) {
            val viewCount = view.childCount
            for (i in 0 until viewCount) {
                destroyView(view.getChildAt(i))
            }
        }
        RouteCore.destroyObject(view)
    }

    //================生命周期相关============
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        //通知栈顶视图
        dispatchEvent(RouteEvents.ON_SHOW, 0)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        dispatchEvent(RouteEvents.ON_HIDE, 0)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        unbindLifecycle()
        dispatchEvent(RouteEvents.ON_DESTROY, 0)
    }

    fun checkChildNum() {
        if (childCount > 1) {
            throw Exception("the view group can only have one child")
        }
    }

    inner class OutAnimationListener(val view: View?) : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {

        }

        override fun onAnimationEnd(animation: Animation?) {
            isAnimationShowing = false
            post {
                if (view != null) {
                    dispatchEvent2Children(RouteEvents.ON_HIDE)
                    RouteCore.dispatchEvent(RouteEvents.ON_HIDE, requestCode, HashMap())
                }
                removeView(view)
                //检查Child数目
                checkChildNum()
            }
        }

        override fun onAnimationStart(animation: Animation?) {
            isAnimationShowing = true
        }
    }

    companion object {
        val EMPTY_MAP = HashMap<String, Any>()
    }
}