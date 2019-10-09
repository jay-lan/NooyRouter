package com.nooy.router.annotation.processor.file

import com.nooy.router.*
import com.nooy.router.annotation.Route
import com.nooy.router.annotation.processor.*
import com.squareup.javapoet.*
import java.lang.StringBuilder
import javax.lang.model.element.*

class RouteMapFile(packageName: String) : BaseFile(packageName) {
    override fun buildFile(): JavaFile {
        val clazz = TypeSpec.classBuilder(CLASS_NAME_ROUTE_MAP)
        clazz.addModifiers(Modifier.FINAL)
        //构造获取路由信息列表的方法
        val methodGetRouteInfoList = MethodSpec.methodBuilder(METHOD_NAME_GET_ROUTE_INFO_LIST)
        methodGetRouteInfoList.addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
            .returns(
                TypeVariableName.get(
                    "ArrayList<RouteInfo>", ClassName.get(ArrayList::class.java), ClassName.get(
                        ROUTER_MODEL_PACKAGE,
                        CLASS_NAME_ROUTE_INFO
                    )
                )
            )
        //方法代码
        val blockGetRouteInfoList = CodeBlock.builder()
            .add(
                "\$T<\$T> list = new \$T();\n",
                ClassName.get(ArrayList::class.java),
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_INFO),
                ClassName.get(ArrayList::class.java)
            )
        for (element in elements) {
            val annotation = element.getAnnotation(Route::class.java)
            var identifier = ""
            var type = RouteTypes.UNKNOWN
            when (element.kind) {
                ElementKind.CLASS -> {
                    val typeElement = element as TypeElement
                    identifier = RouteProcessor.elementUtils.getPackageOf(element).toString() + "." +
                            element.simpleName.toString()
                    val typeMirror = typeElement.superclass
                    typeMirror.kind
                }
                ElementKind.METHOD, ElementKind.FIELD -> {
                    val classElement = element.enclosingElement
                    identifier = RouteProcessor.elementUtils.getPackageOf(element).toString() + "." +
                            classElement.simpleName.toString() + ":" + element.simpleName.toString()
                    if (element.kind == ElementKind.METHOD) {
                        val paramsTypes = StringBuilder()
                        val executableElement = element as ExecutableElement
                        executableElement.parameters?.forEach {
                            it.asType().toString()
                            paramsTypes.append("${it.simpleName}@${it.asType()},")
                        }
                        if (paramsTypes.isNotEmpty()) {
                            identifier = "${identifier}|${paramsTypes.substring(0,paramsTypes.lastIndex)}"
                        }
                    }
                    type = if (element.kind == ElementKind.METHOD) RouteTypes.METHOD else RouteTypes.FIELD
                }
            }
            blockGetRouteInfoList.add(
                "list.add(new \$T(\$S, \$S, \$T.\$L, \$S, \$T.\$L, \$S));\n",
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_INFO),
                annotation.path, annotation.group,
                ClassName.get(ROUTER_PACKAGE, CLASS_NAME_ROUTE_TYPES),
                type, identifier,
                ClassName.get(ROUTER_PACKAGE, CLASS_NAME_ROUTE_MODES),
                annotation.routeMode, annotation.desc
            )
        }
        blockGetRouteInfoList.add("return list;\n")
        methodGetRouteInfoList.addCode(blockGetRouteInfoList.build())
        clazz.addMethod(methodGetRouteInfoList.build())
        return JavaFile.builder(packageName, clazz.build()).build()
    }
}
