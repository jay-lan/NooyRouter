package com.nooy.router.annotation.processor.file

import com.nooy.router.*
import com.nooy.router.annotation.OnRouteEvent
import com.nooy.router.annotation.Param
import com.nooy.router.annotation.Route
import com.nooy.router.annotation.processor.RouteProcessor
import com.squareup.javapoet.*
import java.lang.Exception
import java.lang.StringBuilder
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class RouteEventFile(packageName: String) : BaseFile(packageName) {
    override fun buildFile(): JavaFile {
        val clazz = TypeSpec.classBuilder(CLASS_NAME_ROUTE_EVENT_MAP)
        clazz.addModifiers(Modifier.FINAL)
        //构造获取路由信息列表的方法
        val methodGetRouteInfoList = MethodSpec.methodBuilder(METHOD_NAME_GET_ROUTE_EVENT_LIST)
        methodGetRouteInfoList.addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
            .returns(
                TypeVariableName.get(
                    "ArrayList<RouteEventInfo>", ClassName.get(ArrayList::class.java), ClassName.get(
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
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_EVENT_INFO),
                ClassName.get(ArrayList::class.java)
            )
        for (element in elements) {
            val annotation = element.getAnnotation(OnRouteEvent::class.java)
            //构造类型数组
            val executableElement = element as ExecutableElement
            val params = executableElement.parameters
            val paramTypesBuilder = StringBuilder()
            val paramKeysBuilder = StringBuilder()
            for (paramIndex in params.indices) {
                val param = params[paramIndex]
                val paramAnnotation = param.getAnnotation(Param::class.java)
                val typeName = param.asType().toString()
                val className = if (typeName.indexOf("<") != -1) {
                    typeName.substring(0, typeName.indexOf("<")) + if (typeName.endsWith("[]")) {
                        "[]"
                    } else {
                        ""
                    }
                } else {
                    typeName
                }
                paramTypesBuilder.append("\"$className\",")
                paramKeysBuilder.append("\"${paramAnnotation?.name ?: param.simpleName}\",")
            }
            val paramsTypesString = if (paramTypesBuilder.isNotEmpty()) {
                paramTypesBuilder.deleteCharAt(paramTypesBuilder.lastIndex).toString()
            } else {
                ""
            }
            val paramsKeysString = if (paramKeysBuilder.isNotEmpty()) {
                paramKeysBuilder.deleteCharAt(paramKeysBuilder.lastIndex).toString()
            } else {
                ""
            }
            blockGetRouteInfoList.add(
                "list.add(new \$T(\$S, \$L, \$S, \$S, new String[]{\$L}, new String[]{\$L}));\n",
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_EVENT_INFO),
                annotation.eventName,
                annotation.requestCode,
                element.enclosingElement.asType().toString(),
                element.simpleName,
                paramsTypesString,
                paramsKeysString
            )
        }
        blockGetRouteInfoList.add("return list;\n")
        methodGetRouteInfoList.addCode(blockGetRouteInfoList.build())
        clazz.addMethod(methodGetRouteInfoList.build())
        return JavaFile.builder(packageName, clazz.build()).build()
    }
}