package com.nooy.router.annotation.processor.file

import com.nooy.router.*
import com.nooy.router.annotation.Route
import com.nooy.router.annotation.RouteData
import com.nooy.router.annotation.processor.RouteProcessor
import com.squareup.javapoet.*
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement

class RouteDataFile(packageName: String):BaseFile(packageName) {
    override fun buildFile(): JavaFile {
        val clazz = TypeSpec.classBuilder(CLASS_NAME_ROUTE_DATA_MAP)
        clazz.addModifiers(Modifier.FINAL)
        //构造获取路由信息列表的方法
        val methodGetDataInfoList = MethodSpec.methodBuilder(METHOD_NAME_GET_DATA_INFO_LIST)
        methodGetDataInfoList.addModifiers(Modifier.FINAL, Modifier.STATIC, Modifier.PUBLIC)
            .returns(
                TypeVariableName.get(
                    "ArrayList<RouteDataInfo>", ClassName.get(ArrayList::class.java), ClassName.get(
                        ROUTER_MODEL_PACKAGE,
                        CLASS_NAME_ROUTE_DATA_INFO
                    )
                )
            )
        //方法代码
        val blockGetRouteInfoList = CodeBlock.builder()
            .add(
                "\$T<\$T> list = new \$T();\n",
                ClassName.get(ArrayList::class.java),
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_DATA_INFO),
                ClassName.get(ArrayList::class.java)
            )
        for (element in elements) {
            val annotation = element.getAnnotation(RouteData::class.java)?:continue
            var key = annotation.key
            if (key == "") {
                key = element.simpleName.toString()
            }
            blockGetRouteInfoList.add(
                "list.add(new \$T(\$S, \$S, \$S));\n",
                ClassName.get(ROUTER_MODEL_PACKAGE, CLASS_NAME_ROUTE_DATA_INFO),
                key,element.simpleName.toString(),RouteProcessor.elementUtils.getPackageOf(element).toString()+"."+element.enclosingElement.simpleName
            )
        }
        blockGetRouteInfoList.add("return list;\n")
        methodGetDataInfoList.addCode(blockGetRouteInfoList.build())
        clazz.addMethod(methodGetDataInfoList.build())
        return JavaFile.builder(packageName, clazz.build()).build()
    }

}