package com.nooy.router.utils

import java.lang.Exception
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

object ReflectUtils {
    fun isChildClassOf(clazz: Class<*>, clazz1: Class<*>): Boolean {
        var temp = clazz
        while (!temp.isAssignableFrom(Object::class.java)) {
            if (temp.isAssignableFrom(clazz1)) {
                return true
            } else {
                temp = temp.superclass
            }
        }
        return false
    }

    fun isKotlinClass(clazz: Class<*>): Boolean {
        return clazz.getAnnotation(Metadata::class.java) != null
    }

    fun getStaticValue(clazz: Class<*>, field: Field): Any? {
        field.isAccessible = true
        if (field.modifiers and Modifier.STATIC == Modifier.STATIC) {
            //静态方法
            return field.get(null)
        } else if (isKotlinClass(clazz)) {
            //可能是Kotlin的单例
            val instance = try {
                clazz.getDeclaredField("INSTANCE")
            } catch (e: Exception) {
                return null
            }
            if (instance.modifiers and Modifier.STATIC == Modifier.STATIC && instance.type.canonicalName == clazz.canonicalName) {
                return field.get(instance.get(null))
            }
        }
        return null
    }

    /**
     * 判断传入的类是否是kotlin单例
     */
    fun isKotlinObject(clazz: Class<*>) = isKotlinClass(clazz) && try {
        val instance = clazz.getDeclaredField("INSTANCE")
        instance.modifiers and Modifier.STATIC == Modifier.STATIC && instance.type.canonicalName == clazz.canonicalName
    } catch (e: Exception) {
        false
    }

    fun invokeStaticMethod(clazz: Class<*>, method: Method, vararg params: Any?): Any? {
        method.isAccessible = true
        if (method.modifiers and Modifier.STATIC == Modifier.STATIC) {
            //静态方法
            return method.invoke(null, *params)
        } else if (isKotlinClass(clazz)) {
            //可能是Kotlin的单例
            val instance = try {
                clazz.getDeclaredField("INSTANCE")
            } catch (e: Exception) {
                return null
            }
            if (instance.modifiers and Modifier.STATIC == Modifier.STATIC && instance.type.canonicalName == clazz.canonicalName) {
                return method.invoke(instance.get(null), *params)
            }
        }
        return null
    }

    fun setFieldValue(any: Any, fieldName: String, value: Any) {
        val clazz = any.javaClass
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(any, value)
    }

    fun fromClassName(className: String): Class<*> {
        if (className.endsWith("[]")) {
            //array type
            val realClassName = className.substring(0, className.indexOf("["))
            return Array.newInstance(fromClassName(realClassName), 0)::class.java
        }
        return when (className) {
            "className" -> String::class.java
            "int" -> Int::class.java
            "char" -> Char::class.java
            "byte" -> Byte::class.java
            "long" -> Long::class.java
            "short" -> Short::class.java
            "float" -> Float::class.java
            "double" -> Double::class.java
            "boolean" -> Boolean::class.java
            else -> Class.forName(className)
        }
    }
}