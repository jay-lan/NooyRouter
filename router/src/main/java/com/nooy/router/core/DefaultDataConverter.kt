package com.nooy.router.core

import com.google.gson.Gson
import java.lang.reflect.Type

class DefaultDataConverter : IDataConverter {
    override fun toJson(data: Any): String  = gson.toJson(data)

    override fun <T> fromJson(json: String, clazz: Class<T>): T = gson.fromJson(json, clazz)

    override fun fromJson(json: String, type: Type): Any = gson.fromJson(json, type)

    val gson = Gson()
}