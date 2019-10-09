package com.nooy.router.annotation.processor

import com.nooy.router.annotation.*
import com.nooy.router.annotation.processor.file.RouteDataFile
import com.nooy.router.annotation.processor.file.RouteEventFile
import com.nooy.router.annotation.processor.file.RouteMapFile
import com.nooy.router.annotation.processor.file.RouteViewFile
import java.util.LinkedHashSet
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

class RouteProcessor : AbstractProcessor() {
    companion object {
        lateinit var typeUtils: Types
        lateinit var messager: Messager
        lateinit var filer: Filer
        lateinit var elementUtils: Elements
    }
    var packageName = ""

    @Synchronized
    override fun init(processingEnvironment: ProcessingEnvironment) {
        super.init(processingEnvironment)
        typeUtils = processingEnvironment.typeUtils
        messager = processingEnvironment.messager
        filer = processingEnvironment.filer
        elementUtils = processingEnvironment.elementUtils
    }

    override fun process(elements: MutableSet<out TypeElement>?, environment: RoundEnvironment?): Boolean {
        elements ?: return false
        environment ?: return false
        //获取应用包名
        val applicationAnnotation = environment.getElementsAnnotatedWith(RouteApplication::class.java)
        if (applicationAnnotation.isEmpty()) {
            //error("请用RouteApplication注解修饰您的Application！")
            return false
        }
        val application = applicationAnnotation.first()
        packageName = elementUtils.getPackageOf(application).toString()+".router"

        //处理Route注解
        val routeMapFile = RouteMapFile(packageName)
        for (routeElement in environment.getElementsAnnotatedWith(Route::class.java)) {
            routeMapFile.addElement(routeElement)
        }
        //处理RouteData注解
        val routeDataFile = RouteDataFile(packageName)
        for (routeData in environment.getElementsAnnotatedWith(RouteData::class.java)) {
            routeDataFile.addElement(routeData)
        }
        //处理RouteView注解，主要用于向RouteView中需要获取RouteView的视图注入RouteView实例
        val routeViewFile = RouteViewFile(packageName)
        for (routeData in environment.getElementsAnnotatedWith(RouteViewData::class.java)) {
            routeViewFile.addElement(routeData)
        }
        //处理RouteEvent
        val routeEventFile = RouteEventFile(packageName)
        for (routeEvent in environment.getElementsAnnotatedWith(OnRouteEvent::class.java)) {
            routeEventFile.addElement(routeEvent)
        }
        //代码生成
        routeMapFile.buildFile().writeTo(filer)
        routeDataFile.buildFile().writeTo(filer)
        routeViewFile.buildFile().writeTo(filer)
        routeEventFile.buildFile().writeTo(filer)
        return true
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        val annotations = LinkedHashSet<String>()
        //路由注解
        annotations.add(Route::class.java.canonicalName)
        //路由数据
        annotations.add(RouteData::class.java.canonicalName)
        //路由事件
        annotations.add(OnRouteEvent::class.java.canonicalName)
        return annotations
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun error(e: Element, msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args), e)
    }

    private fun error(msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, *args))
    }

    private fun info(msg: String, vararg args: Any) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, *args))
    }
}