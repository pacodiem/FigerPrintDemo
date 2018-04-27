package com.labs.tekitisoft.figerprintdemo.ui.login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.labs.tekitisoft.figerprintdemo.R
import kotlinx.android.synthetic.main.fragment_new_account.*

/**
 * Created by francisco.dominguez on 27/04/18.
 */
class FragmentNewAccount : Fragment(){

    private lateinit var mAuth : FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        val rootView = inflater!!.inflate(R.layout.fragment_new_account, container, false)
        mAuth = FirebaseAuth.getInstance()

        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button_create_account.setOnClickListener { createAccount(email.text.toString(), pass.text.toString()) }
    }

    fun createAccount(email : String, password : String) {

        Log.d("NEW ACCOUNT", "$email $password")

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(activity,
                {
                    if (it.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("AUTH", "createUserWithEmail:success")
                        val user = mAuth.getCurrentUser();
                        //updateUI(user)
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("AUTH", "createUserWithEmail:failure", it.getException());
                        Toast.makeText(activity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        //updateUI(null);
                    }
                })
    }

    companion object {
        fun newInstance() : FragmentNewAccount = FragmentNewAccount()
    }

}