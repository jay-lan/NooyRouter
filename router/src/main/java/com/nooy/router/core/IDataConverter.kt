package com.nooy.router.core

import java.lang.reflect.Type

interface IDataConverter {
    fun toJson(data: Any): String
    fun <T> fromJson(json: String, clazz: Class<T>): T
    fun fromJson(json: String, type: Type):Any
}