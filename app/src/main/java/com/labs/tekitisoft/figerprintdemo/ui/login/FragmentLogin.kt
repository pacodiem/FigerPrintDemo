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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.labs.tekitisoft.figerprintdemo.FingerprintHandler
import com.labs.tekitisoft.figerprintdemo.R
import com.labs.tekitisoft.figerprintdemo.ui.welcome.FragmentWelcome
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey

/**
 * Created by francisco.dominguez on 18/04/18.
 */
class FragmentLogin : Fragment(){
    private lateinit var fingerprintHandler : FingerprintHandler
    private var cipher: Cipher? = null
    private var keyStore: KeyStore? = null
    private var keyGenerator: KeyGenerator? = null
    private var textView: TextView? = null
    private var textView2: TextView? = null
    private var cryptoObject: FingerprintManager.CryptoObject? = null
    private var fingerprintManager: FingerprintManager? = null
    private var keyguardManager: KeyguardManager? = null
    private var timerDisposable: Disposable? = null

    @TargetApi(Build.VERSION_CODES.M)
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)

        val rootView = inflater!!.inflate(R.layout.fragment_login, container, false)

        // Validar que la version sea al menos M
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Obtener Managers
            keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            fingerprintManager = activity.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager

            textView = rootView.findViewById(R.id.text_view) as TextView
            textView2 = rootView.findViewById(R.id.text_view2) as TextView

            // Validar que el dispositivo tiene sensor de huella digital
            if (!fingerprintManager!!.isHardwareDetected) {
                textView!!.text = "Your device doesn't support fingerprint authentication"
            }

            // Validar que se otorgaron permisos para usar el sensor
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                textView!!.text = "Please enable the fingerprint permission"
            }

            // Validar que se tiene registrada al menos una huella digital
            if (!fingerprintManager!!.hasEnrolledFingerprints()) {
                textView!!.text = "No fingerprint configured. Please register at least one fingerprint in your device's Settings"
            }

            // Validar que se tiene asegurada la pantalla de bloqueo
            if (!keyguardManager!!.isKeyguardSecure) {
                textView!!.text = "Please enable lockscreen security in your device's Settings"
            } else {
                initSensor()
            }

        }
        return rootView
    }

    fun addFragment(){
        val fragmentWelcome = FragmentWelcome.newInstance("Paco")
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
            fingerprintHandler.startAuth(fingerprintManager!!, cryptoObject!!)
        }
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
        fingerprintHandler.cancelFingerPrintSignal()
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