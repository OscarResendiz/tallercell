package com.example.tallercelulares

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Gravity
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
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.Modulos.onbording.BaseTallerActivity
import com.example.tallercelulares.Modulos.onbording.OnboardingActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Entrada y acceso de la aplicación; el asistente vive en el módulo onbording. */
class MainActivity : BaseTallerActivity() {
    private lateinit var repository: RepositorioConfiguracionInicial
    private var deviceAuthSuccess: (() -> Unit)? = null
    private var deviceAuthError: (() -> Unit)? = null

    private val deviceCredentialResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) deviceAuthSuccess?.invoke() else deviceAuthError?.invoke()
        deviceAuthSuccess = null
        deviceAuthError = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeScreen()
        repository = RepositorioConfiguracionInicial(TallerCelularesDatabase.obtenerInstancia(applicationContext))
        showLoading()
        lifecycleScope.launch {
            try {
                when (withContext(Dispatchers.IO) { repository.estado() }) {
                    EstadoConfiguracionInicial.COMPLETA -> showLogin()
                    else -> launchOnboarding()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                showStartupError(exception)
            }
        }
    }

    private fun launchOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    private fun showLoading() {
        render(title = getString(R.string.app_name), subtitle = "Preparando el acceso local...", step = null, primary = null, content = {})
    }

    private fun showStartupError(exception: Exception) {
        render(
            title = "No se pudo iniciar",
            subtitle = "No se pudo consultar la configuración local. Tus datos no se modificaron.",
            step = null, primary = getString(R.string.retry_label),
            onPrimary = {
                showLoading()
                lifecycleScope.launch {
                    try {
                        when (withContext(Dispatchers.IO) { repository.estado() }) {
                            EstadoConfiguracionInicial.COMPLETA -> showLogin()
                            else -> launchOnboarding()
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (retryException: Exception) {
                        showStartupError(retryException)
                    }
                }
            },
            content = {
                addText(
                    this,
                    "Detalle: ${exception.localizedMessage ?: exception.javaClass.simpleName}",
                    color = MUTED
                )
            }
        )
    }

    private fun showLogin(error: String? = null) {
        lifecycleScope.launch {
            val access = try {
                withContext(Dispatchers.IO) { repository.obtenerAcceso() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                showStartupError(exception)
                return@launch
            }
            if (access == null) {
                launchOnboarding()
                return@launch
            }
            if (access.metodo == RepositorioConfiguracionInicial.METODO_CONTRASENA) {
                editFields.clear()
                fieldLayouts.clear()
                render(
                    title = getString(R.string.login_title),
                    subtitle = getString(R.string.login_subtitle),
                    step = null,
                    primary = "Desbloquear",
                    onPrimary = {
                        val password = editFields["login_password"]?.text?.toString().orEmpty().toCharArray()
                        editFields["login_password"]?.text?.clear()
                        lifecycleScope.launch {
                            val authenticated = try {
                                withContext(Dispatchers.Default) {
                                    repository.autenticarContrasena(password)
                                }
                            } catch (cancelled: CancellationException) {
                                password.fill('\u0000')
                                throw cancelled
                            } catch (exception: Exception) {
                                password.fill('\u0000')
                                showMessage(
                                    exception.localizedMessage ?: "No se pudo validar el acceso local."
                                )
                                return@launch
                            }
                            password.fill('\u0000')
                            if (authenticated) showAuthenticated() else showLogin("La contraseña no es correcta.")
                        }
                    },
                    content = {
                        addInput(
                            this,
                            "login_password",
                            getString(R.string.password_label),
                            "",
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                            password = true
                        )
                        error?.let {
                            addSpace(this, 8)
                            addText(this, it, color = ERROR)
                        }
                    }
                )
            } else {
                val method = access.metodo
                render(
                    title = getString(R.string.login_title),
                    subtitle = getString(R.string.login_subtitle),
                    step = null,
                    primary = "Autenticar con ${methodLabel(method)}",
                    onPrimary = {
                        val success = { showAuthenticated() }
                        val failure = { message: String -> showLogin(message) }
                        when (method) {
                            RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID ->
                                confirmDeviceCredential(success, failure)
                            RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID ->
                                confirmBiometric(success, failure)
                            else -> showLogin("El método de acceso guardado no es compatible.")
                        }
                    },
                    content = {
                        addCard(
                            this,
                            "Acceso protegido por Android",
                            "La aplicación no almacena tu PIN, patrón ni información biométrica."
                        )
                        error?.let {
                            addSpace(this, 8)
                            addText(this, it, color = ERROR)
                        }
                    }
                )
            }
        }
    }

    private fun showAuthenticated() {
        render(
            title = getString(R.string.authenticated_title),
            subtitle = getString(R.string.authenticated_body),
            step = null,
            primary = "Bloquear",
            onPrimary = { showLogin() },
            content = {
                addCard(
                    this,
                    "Configuración inicial completada",
                    "Tus datos de configuración están guardados en este dispositivo. No se creó una cuenta en línea."
                )
            }
        )
    }

    private fun confirmDeviceCredential(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        if (!keyguard.isDeviceSecure) {
            onError(getString(R.string.device_unavailable))
            return
        }
        val intent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
            getString(R.string.app_name),
            getString(R.string.system_auth_reason)
        )
        if (intent == null) {
            onError("Android no pudo abrir la confirmación del bloqueo del dispositivo.")
            return
        }
        deviceAuthSuccess = onSuccess
        deviceAuthError = { onError("No se confirmó la identidad. Puedes intentarlo de nuevo o elegir otro método.") }
        deviceCredentialResult.launch(intent)
    }

    private fun confirmBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val availability = BiometricManager.from(this).canAuthenticate(BIOMETRIC_AUTHENTICATORS)
            if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                onError(biometricAvailabilityMessage(availability))
                return
            }
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError(errString.toString())
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric_option))
                .setDescription(getString(R.string.system_auth_reason))
                .setAllowedAuthenticators(BIOMETRIC_AUTHENTICATORS)
                .setNegativeButtonText(getString(R.string.biometric_use_other_method))
                .build()
            prompt.authenticate(promptInfo)
        } catch (exception: SecurityException) {
            Log.e(TAG, "Android denegó iniciar la autenticación biométrica.", exception)
            onError(getString(R.string.biometric_permission_error))
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Android rechazó la configuración del diálogo biométrico.", exception)
            onError(getString(R.string.biometric_configuration_error))
        } catch (exception: IllegalStateException) {
            Log.e(TAG, "No fue posible iniciar el diálogo biométrico.", exception)
            onError(getString(R.string.biometric_start_error))
        }
    }

    private fun biometricAvailabilityMessage(status: Int): String = when (status) {
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            getString(R.string.biometric_no_hardware)
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

    companion object {
        private const val TAG = "TallerCelulares"
        private val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
    }
}
