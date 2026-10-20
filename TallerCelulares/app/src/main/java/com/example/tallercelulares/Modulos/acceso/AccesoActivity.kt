package com.example.tallercelulares.Modulos.acceso

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tallercelulares.ConfiguracionInicial.EstadoConfiguracionInicial
import com.example.tallercelulares.ConfiguracionInicial.RepositorioConfiguracionInicial
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal.ConfiguracionAccesoLocal
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.Modulos.tablero.TableroPrincipal
import com.example.tallercelulares.Modulos.onbording.OnboardingActivity
import com.example.tallercelulares.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entrada local, bloqueo por inactividad y autenticación reciente para operaciones protegidas. */
class AccesoActivity : AppCompatActivity() {
    private lateinit var repository: RepositorioConfiguracionInicial
    private lateinit var root: android.widget.FrameLayout
    private lateinit var session: SesionLocal
    private var accessConfiguration: ConfiguracionAccesoLocal? = null
    private var requestedAuthorization: SolicitudAutenticacionReciente? = null
    private var errorMessage: String? = null
    private var isAuthenticating = false
    private var backgroundedAt: Long? = null
    private var tablero: TableroPrincipal? = null
    private var passwordField: EditText? = null
    private var primaryButton: MaterialButton? = null
    private val lockHandler = android.os.Handler(Looper.getMainLooper())

    private val credentialResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isAuthenticating = false
            if (result.resultCode == RESULT_OK) authenticationSucceeded()
            else showLogin("No se confirmó la identidad. Puedes intentarlo de nuevo.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeSecureWindow()
        repository = RepositorioConfiguracionInicial(
            TallerCelularesDatabase.obtenerInstancia(applicationContext)
        )
        session = SesionLocal(DEFAULT_LOCK_TIMEOUT_MILLIS)

        requestedAuthorization = readAuthorizationRequest(intent)
        if (intent.hasExtra(EXTRA_ACCION) && requestedAuthorization == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        loadLocalConfiguration()
    }

    override fun onStart() {
        super.onStart()
        val backgroundedAt = backgroundedAt
        this.backgroundedAt = null
        if (session.estaAutenticada && backgroundedAt != null &&
            session.haExpirado(SystemClock.elapsedRealtime())
        ) {
            lockSession()
        } else if (session.estaAutenticada) {
            scheduleLockCheck()
            tablero?.refrescar()
        }
    }

    override fun onStop() {
        if (session.estaAutenticada) backgroundedAt = SystemClock.elapsedRealtime()
        lockHandler.removeCallbacks(lockRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        lockHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (session.estaAutenticada && session.haExpirado(SystemClock.elapsedRealtime())) {
            lockSession()
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            session.registrarActividad(SystemClock.elapsedRealtime())
            scheduleLockCheck()
        }
        return super.dispatchTouchEvent(event)
    }

    private fun initializeSecureWindow() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        root = android.widget.FrameLayout(this)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        setContentView(root)
    }

    private fun loadLocalConfiguration() {
        showStatus("Preparando el acceso local…")
        lifecycleScope.launch {
            try {
                val state = withContext(Dispatchers.IO) { repository.estado() }
                if (state != EstadoConfiguracionInicial.COMPLETA) {
                    if (requestedAuthorization != null) {
                        setResult(RESULT_CANCELED)
                        finish()
                    } else {
                        startActivity(Intent(this@AccesoActivity, OnboardingActivity::class.java))
                        finish()
                    }
                    return@launch
                }

                val access = withContext(Dispatchers.IO) { repository.obtenerAcceso() }
                    ?: error("No se encontró el método de acceso configurado.")
                require(access.tiempoBloqueoSegundos >= 0) {
                    "El tiempo de bloqueo configurado no es válido."
                }
                accessConfiguration = access
                session = SesionLocal(access.tiempoBloqueoSegundos * 1_000L)
                showLogin()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "No se pudo preparar el acceso local.", exception)
                showStatus(
                    exception.localizedMessage
                        ?: "No se pudo verificar la configuración de acceso local."
                ) {
                    loadLocalConfiguration()
                }
            }
        }
    }

    private fun showLogin(message: String? = errorMessage) {
        errorMessage = message
        val access = accessConfiguration
        if (access == null) {
            showStatus("No hay un método de acceso local válido.")
            return
        }
        renderScreen(
            title = if (requestedAuthorization == null) {
                getString(R.string.login_title)
            } else {
                "Confirma tu identidad"
            },
            subtitle = if (requestedAuthorization == null) {
                getString(R.string.login_subtitle)
            } else {
                "Para continuar, autentícate antes de " +
                    "${requestedAuthorization?.accion?.descripcion.orEmpty()}."
            },
            content = {
                when (access.metodo) {
                    RepositorioConfiguracionInicial.METODO_CONTRASENA -> addPasswordField()
                    RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID,
                    RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID ->
                        addInformation("La autenticación será confirmada por Android.")
                    else -> addInformation("El método configurado no es compatible.")
                }
                errorMessage?.let { addMessage(it, ERROR) }
            },
            primaryLabel = when (access.metodo) {
                RepositorioConfiguracionInicial.METODO_CONTRASENA -> "Desbloquear"
                else -> "Autenticar con ${methodLabel(access.metodo)}"
            },
            onPrimary = { authenticate(access) }
        )
    }

    private fun addPasswordField() {
        val inputLayout = TextInputLayout(this).apply {
            hint = getString(R.string.password_label)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        val input = TextInputEditText(inputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            isSaveEnabled = false
            isLongClickable = false
        }
        inputLayout.addView(input)
        currentContent?.addView(inputLayout, inputLayoutParams())
        passwordField = input
    }

    private var currentContent: LinearLayout? = null

    private fun authenticate(access: ConfiguracionAccesoLocal) {
        if (isAuthenticating) return
        when (access.metodo) {
            RepositorioConfiguracionInicial.METODO_CONTRASENA -> authenticateWithPassword()
            RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID -> authenticateWithDeviceCredential()
            RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID -> authenticateWithBiometrics()
            else -> showLogin("El método de acceso guardado no es compatible.")
        }
    }

    private fun authenticateWithPassword() {
        val password = passwordField?.text?.toString().orEmpty().toCharArray()
        passwordField?.text?.clear()
        if (password.isEmpty()) {
            password.fill('\u0000')
            showLogin("Escribe tu contraseña para continuar.")
            return
        }
        setAuthenticating(true)
        lifecycleScope.launch {
            val authenticated = try {
                withContext(Dispatchers.Default) {
                    repository.autenticarContrasena(password)
                }
            } catch (cancelled: CancellationException) {
                password.fill('\u0000')
                setAuthenticating(false)
                throw cancelled
            } catch (exception: Exception) {
                password.fill('\u0000')
                Log.e(TAG, "No se pudo verificar la contraseña local.", exception)
                setAuthenticating(false)
                showLogin("No se pudo verificar el acceso. Inténtalo de nuevo.")
                return@launch
            }
            password.fill('\u0000')
            setAuthenticating(false)
            if (authenticated) authenticationSucceeded()
            else showLogin("La contraseña no es correcta.")
        }
    }

    private fun authenticateWithDeviceCredential() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            showLogin(getString(R.string.device_unavailable))
            return
        }
        val confirmation = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.app_name),
            getString(R.string.system_auth_reason)
        )
        if (confirmation == null) {
            showLogin("Android no pudo abrir la confirmación del bloqueo del dispositivo.")
            return
        }
        setAuthenticating(true)
        credentialResult.launch(confirmation)
    }

    private fun authenticateWithBiometrics() {
        try {
            val manager = BiometricManager.from(this)
            val availability = manager.canAuthenticate(BIOMETRIC_AUTHENTICATORS)
            if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                showLogin(biometricAvailabilityMessage(availability))
                return
            }
            setAuthenticating(true)
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        setAuthenticating(false)
                        authenticationSucceeded()
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        setAuthenticating(false)
                        showLogin(errString.toString())
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric_option))
                .setDescription(getString(R.string.system_auth_reason))
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .setNegativeButtonText(getString(android.R.string.cancel))
                .build()
            prompt.authenticate(promptInfo)
        } catch (exception: SecurityException) {
            Log.e(TAG, "Android denegó la autenticación biométrica.", exception)
            setAuthenticating(false)
            showLogin(getString(R.string.biometric_permission_error))
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Android rechazó la configuración biométrica.", exception)
            setAuthenticating(false)
            showLogin(getString(R.string.biometric_configuration_error))
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "No se pudo iniciar la autenticación biométrica.", exception)
            setAuthenticating(false)
            showLogin(getString(R.string.biometric_start_error))
        }
    }

    private fun authenticationSucceeded() {
        val authorizationRequest = requestedAuthorization
        if (authorizationRequest != null) {
            val result = Intent()
                .putExtra(EXTRA_ACCION, authorizationRequest.accion.codigo)
                .putExtra(EXTRA_ID_SOLICITUD, authorizationRequest.idSolicitud)
                .putExtra(EXTRA_AUTORIZADA_EN, SystemClock.elapsedRealtime())
            setResult(RESULT_OK, result)
            finish()
            return
        }

        session.iniciarAhora(SystemClock.elapsedRealtime())
        errorMessage = null
        showAuthenticatedDestination()
        scheduleLockCheck()
    }

    private fun showAuthenticatedDestination() {
        tablero = TableroPrincipal(
            activity = this,
            container = root,
            database = TallerCelularesDatabase.obtenerInstancia(applicationContext),
            alBloquear = { lockSession() }
        )
    }

    private fun lockSession() {
        lockHandler.removeCallbacks(lockRunnable)
        session.bloquear()
        tablero = null
        requestedAuthorization = null
        errorMessage = null
        showLogin()
    }

    private val lockRunnable = object : Runnable {
        override fun run() {
            if (!session.estaAutenticada) return
            val now = SystemClock.elapsedRealtime()
            if (session.haExpirado(now)) lockSession()
            else scheduleLockCheck()
        }
    }

    private fun scheduleLockCheck() {
        lockHandler.removeCallbacks(lockRunnable)
        if (session.estaAutenticada) lockHandler.postDelayed(lockRunnable, LOCK_CHECK_INTERVAL_MILLIS)
    }

    private fun showStatus(message: String, retry: (() -> Unit)? = null) {
        renderScreen(
            title = getString(R.string.app_name),
            subtitle = message,
            content = {},
            primaryLabel = retry?.let { getString(R.string.retry_label) },
            onPrimary = retry
        )
    }

    private fun renderScreen(
        title: String,
        subtitle: String,
        content: LinearLayout.() -> Unit,
        primaryLabel: String?,
        onPrimary: (() -> Unit)?
    ) {
        root.removeAllViews()
        passwordField = null
        primaryButton = null
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(16))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(20))
        }
        addText(header, getString(R.string.brand_name), TEAL, 13f, bold = true)
        addSpace(header, 14)
        addText(header, title, DARK, 26f, bold = true)
        addSpace(header, 8)
        addText(header, subtitle, MUTED, 15f)
        screen.addView(header)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(12))
        }
        currentContent = body
        body.content()
        currentContent = null
        scroll.addView(body)
        screen.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        if (primaryLabel != null) {
            val button = MaterialButton(this).apply {
                text = primaryLabel
                isAllCaps = false
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(TEAL)
                cornerRadius = dp(14)
                setOnClickListener { onPrimary?.invoke() }
            }
            primaryButton = button
            screen.addView(button, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)
            ))
        }
        root.addView(screen)
    }

    private fun setAuthenticating(authenticating: Boolean) {
        isAuthenticating = authenticating
        primaryButton?.isEnabled = !authenticating
    }

    private fun addText(
        container: LinearLayout,
        value: String,
        color: Int,
        size: Float,
        bold: Boolean = false
    ) {
        container.addView(TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        })
    }

    private fun addInformation(message: String) {
        addText(checkNotNull(currentContent), message, MUTED, 15f)
    }

    private fun addMessage(message: String, color: Int) {
        addSpace(checkNotNull(currentContent), 12)
        addText(checkNotNull(currentContent), message, color, 14f)
    }

    private fun addSpace(container: LinearLayout, height: Int) {
        container.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun inputLayoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(12) }

    private fun methodLabel(method: String): String = when (method) {
        RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID ->
            getString(R.string.device_option)
        RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID ->
            getString(R.string.biometric_option)
        else -> getString(R.string.password_option)
    }

    private fun biometricAvailabilityMessage(status: Int): String = when (status) {
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> getString(R.string.biometric_no_hardware)
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            getString(R.string.biometric_hardware_unavailable)
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            getString(R.string.biometric_none_enrolled)
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
            getString(R.string.biometric_security_update_required)
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED ->
            getString(R.string.biometric_unsupported)
        else -> getString(R.string.biometric_unavailable)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun readAuthorizationRequest(intent: Intent): SolicitudAutenticacionReciente? {
        if (!intent.hasExtra(EXTRA_ACCION)) return null
        val action = AccionSensible.desdeCodigo(intent.getStringExtra(EXTRA_ACCION)) ?: return null
        val requestId = intent.getStringExtra(EXTRA_ID_SOLICITUD) ?: return null
        return SolicitudAutenticacionReciente(action, requestId)
    }

    companion object {
        private const val TAG = "AccesoHU002"
        private const val DEFAULT_LOCK_TIMEOUT_MILLIS = 300_000L
        private const val LOCK_CHECK_INTERVAL_MILLIS = 1_000L
        internal const val EXTRA_ACCION = "com.example.tallercelulares.acceso.ACCION"
        internal const val EXTRA_ID_SOLICITUD = "com.example.tallercelulares.acceso.ID_SOLICITUD"
        internal const val EXTRA_AUTORIZADA_EN = "com.example.tallercelulares.acceso.AUTORIZADA_EN"
        private val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
        private val DARK = Color.rgb(20, 32, 48)
        private val MUTED = Color.rgb(91, 105, 122)
        private val TEAL = Color.rgb(0, 117, 112)
        private val ERROR = Color.rgb(176, 42, 55)

        internal fun crearIntentAutenticacion(
            context: Context,
            solicitud: SolicitudAutenticacionReciente
        ): Intent = Intent(context, AccesoActivity::class.java)
            .putExtra(EXTRA_ACCION, solicitud.accion.codigo)
            .putExtra(EXTRA_ID_SOLICITUD, solicitud.idSolicitud)
    }
}

private val AccionSensible.descripcion: String
    get() = when (this) {
        AccionSensible.VER_CREDENCIAL_EQUIPO -> "consultar la credencial del equipo"
        AccionSensible.EXPORTAR_RESPALDO -> "exportar el respaldo"
        AccionSensible.RESTAURAR_RESPALDO -> "restaurar el respaldo"
        AccionSensible.BORRAR_DATOS -> "borrar los datos"
    }

/**
 * Componente launcher sin datos protegidos ni resultados de autenticación hacia apps externas.
 * Las solicitudes de autorización sensibles solo pueden dirigirse a AccesoActivity, no exportada.
 */
class InicioAccesoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, AccesoActivity::class.java))
        finish()
    }
}
