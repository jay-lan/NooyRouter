package com.nooy.router.core

import android.app.Activity
import android.content.Intent
import android.os.Parcelable
import com.nooy.router.annotation.DataInjector
import com.nooy.router.model.RouteMate
import java.io.Serializable
import java.lang.Exception

object DataInjector {

    var dataConverter: IDataConverter = DefaultDataConverter()

    fun injectObject(any: Any, dataMap: Map<String, Any?>) {
        val className = any.javaClass.canonicalName ?: ""
        val dataList = RouteCore.routeDataMap[className]
        val clazz = any.javaClass
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

    fun injectActivity(activity: Activity) {
        val intent = activity.intent
        val className = activity.javaClass.canonicalName ?: ""
        val dataList = RouteCore.routeDataMap[className]
        val clazz = activity.javaClass
        for (dataInfo in dataList) {
            if (!intent.hasExtra(dataInfo.key)) continue
            try {
                val field = clazz.getDeclaredField(dataInfo.fieldName)
                field.isAccessible = true
                when (field.type) {
                    Int::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toIntOrNull()
                            ?: intent.getIntExtra(dataInfo.key, 0)
                    )
                    Short::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toShortOrNull()
                            ?: intent.getShortExtra(dataInfo.key, 0)
                    )
                    Byte::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toByteOrNull()
                            ?: intent.getByteExtra(dataInfo.key, 0)
                    )
                    Char::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toCharArray()?.get(0)
                            ?: intent.getCharExtra(dataInfo.key, 0.toChar())
                    )
                    Long::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toLongOrNull()
                            ?: intent.getLongExtra(dataInfo.key, 0)
                    )
                    Double::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toDoubleOrNull()
                            ?: intent.getDoubleExtra(dataInfo.key, 0.0)
                    )
                    Float::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toFloatOrNull()
                            ?: intent.getFloatExtra(dataInfo.key, 0F)
                    )
                    Boolean::class.java -> field.set(
                        activity, intent.getStringExtra(dataInfo.key)?.toBoolean()
                            ?: intent.getBooleanExtra(dataInfo.key, false)
                    )
                    IntArray::class.java -> field.set(activity, intent.getIntArrayExtra(dataInfo.key))
                    ShortArray::class.java -> field.set(activity, intent.getShortArrayExtra(dataInfo.key))
                    ByteArray::class.java -> field.set(activity, intent.getByteArrayExtra(dataInfo.key))
                    CharArray::class.java -> field.set(activity, intent.getCharArrayExtra(dataInfo.key))
                    LongArray::class.java -> field.set(activity, intent.getLongArrayExtra(dataInfo.key))
                    DoubleArray::class.java -> field.set(activity, intent.getDoubleArrayExtra(dataInfo.key))
                    FloatArray::class.java -> field.set(activity, intent.getFloatArrayExtra(dataInfo.key))
                    BooleanArray::class.java -> field.set(activity, intent.getBooleanArrayExtra(dataInfo.key))
                    Serializable::class.java -> field.set(activity, intent.getSerializableExtra(dataInfo.key))
                    Parcelable::class.java -> field.set(activity, intent.getParcelableExtra(dataInfo.key))
                    Array<Parcelable>::class.java -> field.set(activity, intent.getParcelableArrayExtra(dataInfo.key))
                    Array<String>::class.java -> field.set(activity, intent.getStringArrayExtra(dataInfo.key))
                    else -> {
                        field.set(
                            activity,
                            dataConverter.fromJson(intent.getStringExtra(dataInfo.key), field.genericType)
                        )
                    }
                }
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
    }

    fun writeData2Intent(intent: Intent, routeMate: RouteMate) {
        routeMate.dataMap.forEach {
            //判断data类型是否是intent能放入的类型
            val intentClass = Intent::class.java
            val key = it.key
            when (val value = it.value) {
                is Int, is Short, is Boolean, is Byte, is Char, is Float, is Double, is Long, is String -> {
                    intent.putExtra(key, value.toString())
                }
                is Parcelable -> intent.putExtra(key, value)
                is Serializable -> intent.putExtra(key, value)
                is IntArray -> intent.putExtra(key, value)
                is ShortArray -> intent.putExtra(key, value)
                is BooleanArray -> intent.putExtra(key, value)
                is ByteArray -> intent.putExtra(key, value)
                is CharArray -> intent.putExtra(key, value)
                is FloatArray -> intent.putExtra(key, value)
                is DoubleArray -> intent.putExtra(key, value)
                is LongArray -> intent.putExtra(key, value)
                else -> {
                    intent.putExtra(key, dataConverter.toJson(value))
                }
            }
            try {
                val method = intentClass.getDeclaredMethod("putExtra", String::class.java, it.value.javaClass)
                method.invoke(intent, it.key, it.value)
            } catch (e: NoSuchMethodException) {
                //这个数据类型不能放入Intent的extra中，需要使用类型转换器转换为可放入intent中的类型
                intent.putExtra(it.key, dataConverter.toJson(it.value))
            }
        }
    }
}