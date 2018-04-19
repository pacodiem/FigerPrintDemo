package com.labs.tekitisoft.figerprintdemo


import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.labs.tekitisoft.figerprintdemo.ui.login.FragmentLogin

class MainActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentLogin = FragmentLogin.newInstance()

        supportFragmentManager // use this instead normal fragment manager to change viewPager successfully
                .beginTransaction()
                .replace(R.id.container, fragmentLogin)
                //.addToBackStack(null)
                .commit()
    }
}