package com.labs.tekitisoft.figerprintdemo

import android.Manifest
import android.widget.Toast
import android.hardware.fingerprint.FingerprintManager
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.USE_FINGERPRINT
import android.annotation.TargetApi
import android.os.CancellationSignal
import android.support.v4.app.ActivityCompat
import android.content.Context
import android.os.Build
import android.support.v4.app.Fragment
import android.util.Log
import com.labs.tekitisoft.figerprintdemo.ui.login.FragmentLogin


/**
 * Created by francisco.dominguez on 16/04/18.
 */
@TargetApi(Build.VERSION_CODES.M)
class FingerprintHandler(private val context: Context, private val fragment : FragmentLogin) : FingerprintManager.AuthenticationCallback() {

    // Se debe usar cancellationSignal cuando la app ya no necesite leer la huella, por ejemplo cuando se va al backgrpound
    // Si no se usa este metodo, las otras apps no podran usar el sensor de lector de huellas
    private var cancellationSignal: CancellationSignal? = null

    fun startAuth(manager: FingerprintManager, cryptoObject: FingerprintManager.CryptoObject) {
        cancellationSignal = CancellationSignal()
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
            manager.authenticate(cryptoObject, cancellationSignal, 0, this, null)
        }
    }

    fun cancelFingerPrintSignal(){
        cancellationSignal
        //TODO ver como se hace la cancelacion de la senal y llamar al metodo desde el fragment destruction
        // TODO investigar que metodo del fragmento se llama aldestruir o salir de la app o al ponerla en el back
    }

    override fun onAuthenticationError(errMsgId: Int,
                                       errString: CharSequence) {
        Toast.makeText(context,
                "${context.getString(R.string.auth_message1)}\n$errString",
                Toast.LENGTH_LONG).show()
        fragment.resetSensor()
    }

    override fun onAuthenticationFailed() {
        Toast.makeText(context,
                "${context.getString(R.string.auth_message2)}",
                Toast.LENGTH_LONG).show()
    }

    override fun onAuthenticationHelp(helpMsgId: Int,
                                      helpString: CharSequence) {
        Toast.makeText(context,
                "${context.getString(R.string.auth_message3)}\n$helpString",
                Toast.LENGTH_LONG).show()
    }


    override fun onAuthenticationSucceeded(
            result: FingerprintManager.AuthenticationResult) {

        Toast.makeText(context,
                "${context.getString(R.string.auth_message4)}",
                Toast.LENGTH_LONG).show()
        fragment.addFragment()
    }

}