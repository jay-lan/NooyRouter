package com.nooy.router.sample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nooy.router.Router
import com.nooy.router.annotation.Route
import com.nooy.router.sample.constants.RoutePath
import com.nooy.router.sample.entity.Person
import kotlinx.android.synthetic.main.activity_main.*

@Route(RoutePath.mainActivity)
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_main)
        super.onCreate(savedInstanceState)
        bindEvents()
    }

    fun bindEvents() {
        navigateActivity.setOnClickListener {
            Router.to(RoutePath.secondActivity)
                .withData("text" to "内容", "number" to 233,"bool" to true,"intArray" to IntArray(2){it},
                    "personList"  to arrayListOf(Person("dealin"),Person("233")))
                .navigate()
            /*startActivity(Intent(this,SecondActivity::class.java).apply {
                putExtra("number", 233)
                putExtra("text","内容")
            })*/
        }
        MyView(this,0)
    }
}
