package com.labs.tekitisoft.figerprintdemo.ui.welcome

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.labs.tekitisoft.figerprintdemo.R
import kotlinx.android.synthetic.main.fragment_welcome.view.*


/**
 * Created by francisco.dominguez on 18/04/18.
 */
class FragmentWelcome : Fragment(){

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_welcome, container, false)
        rootView.userName.text = "Bienvenido ${arguments.getString(USERNAME_VAL)}"
        return rootView
    }

    companion object {
        val USERNAME_VAL= "username"

        fun newInstance(userName : String) : FragmentWelcome{
            val args = Bundle()
            args.putString(USERNAME_VAL, userName)
            val fragment = FragmentWelcome()
            fragment.arguments = args
            return fragment
        }
    }
}