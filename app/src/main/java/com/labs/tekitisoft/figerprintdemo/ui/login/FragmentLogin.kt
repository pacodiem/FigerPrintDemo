package com.labs.tekitisoft.figerprintdemo.ui.login

import android.Manifest
import android.annotation.TargetApi
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.labs.tekitisoft.figerprintdemo.FingerprintHandler
import com.labs.tekitisoft.figerprintdemo.R
import com.labs.tekitisoft.figerprintdemo.ui.welcome.FragmentWelcome
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.fragment_new_account.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

/**
 * Created by francisco.dominguez on 18/04/18.
 */
class FragmentLogin : Fragment(){
    private var fingerprintHandler : FingerprintHandler? = null
    private var cipher: Cipher? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var textView: TextView? = null
    private var textView2: TextView? = null
    private var cryptoObject: FingerprintManager.CryptoObject? = null
    private var fingerprintManager: FingerprintManager? = null
    private var keyguardManager: KeyguardManager? = null
    private var timerDisposable: Disposable? = null
    private lateinit var mAuth : FirebaseAuth

    @TargetApi(Build.VERSION_CODES.M)
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val rootView = inflater!!.inflate(R.layout.fragment_login, container, false)

        mAuth = FirebaseAuth.getInstance()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth.currentUser

        // Prueba de uso de flatMap y switch Map (no tienen nada que ver con el demo)
        /*val list = listOf("Alpha", "Beta", "Gamma", "Delta", "Epsilon")

        val scheduler = TestScheduler()

        Observable.fromArray(list)
                .flatMapIterable{list}
                .flatMap{
                    val delay = Random().nextInt(10).toLong()
                    Observable.just("$it X").delay(delay,TimeUnit.SECONDS)
                }
                //.toList()
                //.map{Log.d("RX MAP","$it")}
                .doOnNext{Log.d(" ******* RX NEXT ******" ,"$it")}
                .subscribe{Log.d("RX SUBSCRIBE","$it")}

        Log.d("TEST", "Working")*/

        // Validar que la version sea al menos M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Obtener Managers
            keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            fingerprintManager = activity.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

            textView = rootView.findViewById(R.id.text_view) as TextView
            textView2 = rootView.findViewById(R.id.text_view2) as TextView

            // Validar que el dispositivo tiene sensor de huella digital
            if (!fingerprintManager!!.isHardwareDetected) {
                textView!!.text = "Tu dispositivo no soporta autentificacion con huella digital"
            }

            // Validar que se otorgaron permisos para usar el sensor
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                textView!!.text = "Por favor otorga permisos de lectura de huella digital"
            }

            // Validar que se tiene registrada al menos una huella digital
            if (!fingerprintManager!!.hasEnrolledFingerprints()) {
                textView!!.text = "No hay huella digital configurada"
            }

            // Validar que se tiene asegurada la pantalla de bloqueo
            if (!keyguardManager!!.isKeyguardSecure) {
                textView!!.text = "Por favor activa el candado de pantalla en tu dispositivo"
            } else {
                initSensor()
            }

        }
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Habilitando boton
        //val btn_click_me = rootView.findViewById(R.id.create_account) as TextView
        create_account.setOnClickListener{showNewAccountFragment()}
        button_login.setOnClickListener { signInWithEmailAndPassword(email_login.text.toString(), pass_login.text.toString()) }
    }

    fun showNewAccountFragment(){
        val fragmentWelcome = FragmentNewAccount.newInstance()
        activity.supportFragmentManager // use this instead normal fragment manager to change viewPager successfully
                .beginTransaction()
                .replace(R.id.container, fragmentWelcome)
                .addToBackStack(null)
                .commit()
    }

    fun addFragment(userName : String){
        val fragmentWelcome = FragmentWelcome.newInstance(userName)
        activity.supportFragmentManager // use this instead normal fragment manager to change viewPager successfully
                .beginTransaction()
                .replace(R.id.container, fragmentWelcome)
                .addToBackStack(null)
                .commit()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun initSensor() {
        try {
            generateKey()
        } catch (e: FingerprintException) {
            e.printStackTrace()
        }

        if (initCipher()) {
            // Se crea el CryptoObject si no hubo problema al inicializarlo
            cryptoObject = FingerprintManager.CryptoObject(cipher!!)
            // Iniciar el proceso de autentificacion
            fingerprintHandler = FingerprintHandler(context, this)
            fingerprintHandler?.startAuth(fingerprintManager!!, cryptoObject!!)
        }
    }

    fun signInWithEmailAndPassword(email : String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity,
                {
                    if (it.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("LOGIN", "signInWithEmail:success")
                        val user = mAuth.getCurrentUser() as FirebaseUser
                        Log.d("LOGIN", "${user.email}")
                        addFragment("${user.email}")
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(activity, "Usuario o password incorrecto",
                                Toast.LENGTH_SHORT).show();
                        //updateUI(null)
                    }
                })
    }

    fun resetSensor(){
        textView2?.text = "${getString(R.string.message1)}"
        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext({
                    textView2?.text = "${getString(R.string.message1)} \n ${getString(R.string.message2).replace("#", "${TIMER_VALUE - it}")}"
                })
                .takeUntil({aLong -> aLong == TIMER_VALUE})
                .doOnComplete({
                    initSensor()
                    textView2?.text = "${getString(R.string.message3)}"
                }).subscribe()
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Throws(FingerprintException::class)
    private fun generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyStore!!.load(null)
            keyGenerator!!.init(KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())

            keyGenerator!!.generateKey()
        } catch (exc: KeyStoreException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        } catch (exc: NoSuchAlgorithmException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        } catch (exc: NoSuchProviderException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        } catch (exc: InvalidAlgorithmParameterException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        } catch (exc: CertificateException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        } catch (exc: IOException) {
            exc.printStackTrace()
            throw FingerprintException(exc)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun initCipher():Boolean {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get Cipher", e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException("Failed to get Cipher", e)
        }

        try {
            keyStore!!.load(null)
            val key = keyStore!!.getKey(KEY_NAME, null) as SecretKey
            cipher!!.init(Cipher.ENCRYPT_MODE, key)
            return true
        } catch (e: KeyPermanentlyInvalidatedException) {
            return false
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: UnrecoverableKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: IOException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to init Cipher", e)
        } catch (e: InvalidKeyException) {
            throw RuntimeException("Failed to init Cipher", e)
        }
    }


    override fun onPause() {
        super.onPause()
        if (fingerprintHandler != null)
            fingerprintHandler?.cancelFingerPrintSignal()

    }

    override fun onDestroy() {
        super.onDestroy()
        if (timerDisposable != null) {
            timerDisposable?.dispose()
        }
    }

    private inner class FingerprintException(e:Exception):Exception(e)

    companion object {
        const val KEY_NAME = "yourKey"
        const val TIMER_VALUE : Long = 30

        fun newInstance() : FragmentLogin = FragmentLogin()
    }
}