package com.nooy.router.annotation.processor.file

import com.squareup.javapoet.JavaFile
import javax.lang.model.element.Element

abstract class BaseFile(val packageName:String) {
    val elements = ArrayList<Element>()
    abstract fun buildFile():JavaFile
    fun addElement(element: Element) {
        elements.add(element)
    }
}