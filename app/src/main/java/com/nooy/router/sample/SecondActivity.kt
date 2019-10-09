package com.nooy.router.sample

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import com.nooy.router.annotation.Route
import com.nooy.router.annotation.RouteData
import com.nooy.router.core.DataInjector
import com.nooy.router.sample.constants.RoutePath
import com.nooy.router.sample.entity.Person

import kotlinx.android.synthetic.main.activity_second.*

@Route(RoutePath.secondActivity)
class SecondActivity : AppCompatActivity() {

    @RouteData
    lateinit var text:String

    @RouteData("number")
    var num: Int = 0

    @RouteData
    var bool:Boolean = false
    @RouteData
    lateinit var intArray: IntArray
    @RouteData
    lateinit var personList:ArrayList<Person>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        setSupportActionBar(toolbar)
        fab.setOnClickListener { view ->
            personList.forEach {
                println(it.name)
            }
            Snackbar.make(view, "Bool:$bool", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }
}
