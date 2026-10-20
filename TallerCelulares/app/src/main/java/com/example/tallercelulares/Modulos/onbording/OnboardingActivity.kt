package com.example.tallercelulares.Modulos.onbording

import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SearchView
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
import com.example.tallercelulares.ConfiguracionInicial.ConfiguracionInicialValidator
import com.example.tallercelulares.ConfiguracionInicial.DatosTaller
import com.example.tallercelulares.ConfiguracionInicial.EstadoConfiguracionInicial
import com.example.tallercelulares.ConfiguracionInicial.RepositorioConfiguracionInicial
import com.example.tallercelulares.ConfiguracionInicial.ResultadoValidacionTaller
import com.example.tallercelulares.ConfiguracionInicial.VerificadorContrasena
import com.example.tallercelulares.ConfiguracionInicial.VerificadorDerivado
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal.ConfiguracionAccesoLocal
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTaller
import com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema.CatalogoMonedaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema.CatalogoZonaHorariaSistema
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.Instant
import java.util.Currency
import java.util.Locale
import java.util.TimeZone
import com.example.tallercelulares.Modulos.acceso.AccesoActivity
import com.example.tallercelulares.R

/** Controla el asistente de primer uso, paso a paso. */
class OnboardingActivity : BaseTallerActivity() {
    private lateinit var repository: RepositorioConfiguracionInicial
    private var monedasSistema: List<CatalogoMonedaSistema> = emptyList()
    private var zonasHorariasSistema: List<CatalogoZonaHorariaSistema> = emptyList()
    private var selectedMethod = RepositorioConfiguracionInicial.METODO_CONTRASENA
    private var pendingAccess: ConfiguracionAccesoLocal? = null
    private var deviceAuthSuccess: (() -> Unit)? = null
    private var deviceAuthError: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeScreen()
        repository = RepositorioConfiguracionInicial(TallerCelularesDatabase.obtenerInstancia(applicationContext))
        savedInstanceState?.keySet()?.filter { it.startsWith(FIELD_STATE_PREFIX) }?.forEach { key ->
            val field = key.removePrefix(FIELD_STATE_PREFIX)
            if (field !in SENSITIVE_FIELD_KEYS) fieldValues[field] = savedInstanceState.getString(key).orEmpty()
        }
        selectedMethod = savedInstanceState?.getString(STATE_METHOD) ?: RepositorioConfiguracionInicial.METODO_CONTRASENA
        showLoading()
        lifecycleScope.launch {
            try {
                val savedWorkshop = withContext(Dispatchers.IO) { repository.obtenerTaller() }
                val catalogs = withContext(Dispatchers.IO) {
                    repository.prepararCatalogosSistema(
                        monedaDispositivo = defaultCurrencyCode(),
                        zonaHorariaDispositivo = TimeZone.getDefault().id,
                        monedaConfigurada = savedWorkshop?.monedaCodigo,
                        zonaHorariaConfigurada = savedWorkshop?.zonaHoraria
                    )
                }
                monedasSistema = catalogs.first
                zonasHorariasSistema = catalogs.second
                when (withContext(Dispatchers.IO) { repository.estado() }) {
                    EstadoConfiguracionInicial.COMPLETA -> returnToLogin()
                    EstadoConfiguracionInicial.INCOMPLETA -> {
                        if (savedWorkshop != null && fieldValues.isEmpty()) populateForm(savedWorkshop)
                        showWelcome(incomplete = true)
                    }
                    EstadoConfiguracionInicial.SIN_CONFIGURACION -> showWelcome()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                showStartupError(exception)
            }
        }
    }

    // Al finalizar, el acceso se abre en la actividad de entrada.
    private fun returnToLogin() {
        startActivity(Intent(this, AccesoActivity::class.java))
        finish()
    }

    // Registra el resultado de confirmar el bloqueo del dispositivo.
    private val deviceCredentialResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                deviceAuthSuccess?.invoke()
            } else {
                deviceAuthError?.invoke()
            }
            deviceAuthSuccess = null
            deviceAuthError = null
        }

    // Conserva datos no sensibles al recrear la actividad, nunca contraseñas.
    override fun onSaveInstanceState(outState: Bundle) {
        captureCurrentFields()
        fieldValues
            .filterKeys { it !in SENSITIVE_FIELD_KEYS }
            .forEach { (key, value) -> outState.putString(FIELD_STATE_PREFIX + key, value) }
        outState.putString(STATE_METHOD, selectedMethod)
        super.onSaveInstanceState(outState)
    }

    private fun showLoading() {
        render(
            title = getString(R.string.app_name),
            subtitle = "Preparando el almacenamiento local…",
            step = null,
            primary = null,
            content = {}
        )
    }

    private fun showStartupError(exception: Exception) {
        render(
            title = "No se pudo iniciar",
            subtitle = "No se pudo consultar la configuración local. Tus datos no se modificaron.",
            step = null,
            primary = getString(R.string.retry_label),
            onPrimary = {
                showLoading()
                lifecycleScope.launch {
                    try {
                        val savedWorkshop = withContext(Dispatchers.IO) {
                            repository.obtenerTaller()
                        }
                        val catalogs = withContext(Dispatchers.IO) {
                            repository.prepararCatalogosSistema(
                                monedaDispositivo = defaultCurrencyCode(),
                                zonaHorariaDispositivo = TimeZone.getDefault().id,
                                monedaConfigurada = savedWorkshop?.monedaCodigo,
                                zonaHorariaConfigurada = savedWorkshop?.zonaHoraria
                            )
                        }
                        monedasSistema = catalogs.first
                        zonasHorariasSistema = catalogs.second
                        when (withContext(Dispatchers.IO) { repository.estado() }) {
                            EstadoConfiguracionInicial.COMPLETA -> returnToLogin()
                            EstadoConfiguracionInicial.INCOMPLETA -> {
                                if (savedWorkshop != null && fieldValues.isEmpty()) populateForm(savedWorkshop)
                                showWelcome(incomplete = true)
                            }
                            EstadoConfiguracionInicial.SIN_CONFIGURACION -> showWelcome()
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

    private fun showWelcome(incomplete: Boolean = false) {
        render(
            title = getString(R.string.welcome_title),
            subtitle = getString(R.string.welcome_subtitle),
            step = "PASO 1 DE 4",
            primary = getString(R.string.continue_label),
            onPrimary = { showAccessMethod() },
            content = {
                addCard(
                    this,
                    getString(R.string.local_data_title),
                    getString(R.string.local_data_body)
                )
                addSpace(this, 12)
                addCard(
                    this,
                    getString(R.string.recovery_title),
                    getString(R.string.recovery_body)
                )
                if (incomplete) {
                    addSpace(this, 12)
                    addCard(
                        this,
                        "Configuración pendiente",
                        "Se encontró una configuración parcial. Completa el asistente para corregirla; no se reemplazará una configuración que ya esté completa."
                    )
                }
            }
        )
    }

    private fun showAccessMethod(errorMessage: String? = null) {
        render(
            title = getString(R.string.access_title),
            subtitle = getString(R.string.access_subtitle),
            step = "PASO 2 DE 4",
            primary = getString(R.string.next_label),
            secondary = getString(R.string.back_label),
            onSecondary = { showWelcome() },
            onPrimary = {
                when (selectedMethod) {
                    RepositorioConfiguracionInicial.METODO_CONTRASENA -> showPasswordSetup()
                    RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID ->
                        confirmDeviceCredential(
                            onSuccess = {
                                pendingAccess = accessConfiguration(selectedMethod)
                                showWorkshop()
                            },
                            onError = { message ->
                                showAccessMethod(message)
                            }
                        )
                    RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID ->
                        confirmBiometric(
                            onSuccess = {
                                pendingAccess = accessConfiguration(selectedMethod)
                                showWorkshop()
                            },
                            onError = { message ->
                                showAccessMethod(message)
                            }
                        )
                }
            },
            content = {
                val methodGroup = RadioGroup(this@OnboardingActivity).apply {
                    orientation = RadioGroup.VERTICAL
                    addView(methodOption(
                        RepositorioConfiguracionInicial.METODO_CONTRASENA,
                        getString(R.string.password_option),
                        enabled = true
                    ))
                    addView(methodOption(
                        RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID,
                        getString(R.string.device_option),
                        enabled = deviceCredentialAvailable()
                    ))
                    addView(methodOption(
                        RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID,
                        getString(R.string.biometric_option),
                        enabled = biometricAvailable()
                    ))
                    setOnCheckedChangeListener { radioGroup, checkedId ->
                        val selected = radioGroup.findViewById<RadioButton>(checkedId)
                        selected?.tag?.let { selectedMethod = it.toString() }
                    }
                    check(methodId(selectedMethod))
                }
                addView(methodGroup)
                addSpace(this, 12)
                addCard(
                    this,
                    "Sin cuenta en línea",
                    "La opción de bloqueo o biometría utiliza la autenticación de Android. Taller Celulares no almacena tu PIN, patrón ni datos biométricos."
                )
                errorMessage?.let {
                    addSpace(this, 8)
                    addText(this, it, color = ERROR)
                }
            }
        )
    }

    private fun methodOption(method: String, label: String, enabled: Boolean): RadioButton =
        RadioButton(this).apply {
            id = methodId(method)
            tag = method
            text = label
            isEnabled = enabled
            textSize = 16f
            setPadding(dp(4), dp(10), dp(4), dp(10))
            isChecked = method == selectedMethod
        }

    private fun methodId(method: String): Int = when (method) {
        RepositorioConfiguracionInicial.METODO_CONTRASENA -> 101
        RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID -> 102
        else -> 103
    }

    private fun deviceCredentialAvailable(): Boolean =
        (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure

    private fun biometricAvailable(): Boolean =
        try {
            BiometricManager.from(this).canAuthenticate(BIOMETRIC_AUTHENTICATORS) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } catch (exception: SecurityException) {
            Log.e(TAG, "No fue posible comprobar la disponibilidad biométrica.", exception)
            false
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "Android rechazó la configuración de autenticadores biométricos.", exception)
            false
        }

    private fun showPasswordSetup(error: String? = null) {
        editFields.clear()
        fieldLayouts.clear()
        render(
            title = "Crea tu contraseña",
            subtitle = getString(R.string.password_requirements),
            step = "PASO 2 DE 4",
            primary = getString(R.string.next_label),
            secondary = getString(R.string.back_label),
            onSecondary = { showAccessMethod() },
            onPrimary = {
                val password = editFields["password"]?.text?.toString().orEmpty().toCharArray()
                val confirmation = editFields["password_confirm"]?.text?.toString().orEmpty().toCharArray()
                editFields["password"]?.text?.clear()
                editFields["password_confirm"]?.text?.clear()
                if (password.size !in 10..128) {
                    password.fill('\u0000')
                    confirmation.fill('\u0000')
                    showPasswordSetup("La contraseña debe tener entre 10 y 128 caracteres.")
                } else if (!password.contentEquals(confirmation)) {
                    password.fill('\u0000')
                    confirmation.fill('\u0000')
                    showPasswordSetup("Las contraseñas no coinciden. Inténtalo de nuevo.")
                } else {
                    confirmation.fill('\u0000')
                    lifecycleScope.launch {
                        try {
                            val derived = try {
                                withContext(Dispatchers.Default) {
                                    VerificadorContrasena.derivar(password)
                                }
                            } finally {
                                password.fill('\u0000')
                            }
                            pendingAccess = accessConfiguration(
                                RepositorioConfiguracionInicial.METODO_CONTRASENA,
                                derived
                            )
                            showWorkshop()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (exception: Exception) {
                            showPasswordSetup(
                                exception.localizedMessage ?: "No se pudo proteger la contraseña."
                            )
                        }
                    }
                }
            },
            content = {
                addInput(
                    this,
                    "password",
                    getString(R.string.password_label),
                    "",
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                    password = true
                )
                addSpace(this, 12)
                addInput(
                    this,
                    "password_confirm",
                    getString(R.string.password_confirm_label),
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
    }

    private fun showWorkshop() {
        captureCurrentFields()
        editFields.clear()
        fieldLayouts.clear()
        val defaults = defaultFormValues()
        render(
            title = getString(R.string.shop_title),
            subtitle = getString(R.string.shop_subtitle),
            step = "PASO 3 DE 4",
            primary = getString(R.string.review_label),
            secondary = getString(R.string.back_label),
            onSecondary = {
                captureCurrentFields()
                if (selectedMethod == RepositorioConfiguracionInicial.METODO_CONTRASENA) {
                    showPasswordSetup()
                } else {
                    showAccessMethod()
                }
            },
            onPrimary = {
                captureCurrentFields()
                when (val result = ConfiguracionInicialValidator.validar(datosTaller())) {
                    is ResultadoValidacionTaller.Invalido -> {
                        fieldLayouts[result.campo]?.error = result.mensaje
                            ?: "Revisa este dato."
                        showMessage(result.mensaje)
                    }
                    is ResultadoValidacionTaller.Valido -> {
                        showReview(result.configuracion)
                    }
                }
            },
            content = {
                addSectionTitle(this, "Identidad del taller")
                addInput(this, "nombre", getString(R.string.shop_name), defaults.getValue("nombre"))
                addInput(
                    this,
                    "telefono",
                    getString(R.string.shop_phone),
                    defaults.getValue("telefono"),
                    InputType.TYPE_CLASS_PHONE
                )
                addInput(this, "direccion", getString(R.string.shop_address), defaults.getValue("direccion"))
                addSectionTitle(this, "Formato y folios")
                addSelectionDropdown(
                    this,
                    key = "moneda",
                    label = getString(R.string.currency_label),
                    selectedValue = defaults.getValue("moneda"),
                    options = monedasSistema.map {
                        SelectionOption(it.codigo, "${it.nombre} (${it.simbolo} · ${it.codigo})")
                    }
                )
                addSelectionDropdown(
                    this,
                    key = "zona_horaria",
                    label = getString(R.string.timezone_label),
                    selectedValue = defaults.getValue("zona_horaria"),
                    options = zonasHorariasSistema.map {
                        SelectionOption(it.id, "${it.nombre} (${it.id})")
                    }
                )
                addInput(this, "prefijo_folio", getString(R.string.folio_prefix_label), defaults.getValue("prefijo_folio"))
                addInput(
                    this,
                    "siguiente_folio",
                    getString(R.string.next_folio_label),
                    defaults.getValue("siguiente_folio"),
                    InputType.TYPE_CLASS_NUMBER
                )
                addSectionTitle(this, "Condiciones iniciales")
                addInput(
                    this,
                    "impuesto",
                    getString(R.string.tax_percent_label),
                    defaults.getValue("impuesto"),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                )
                addInput(
                    this,
                    "cargo_diagnostico",
                    getString(R.string.diagnostic_fee_label),
                    defaults.getValue("cargo_diagnostico"),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                )
                addInput(
                    this,
                    "vigencia",
                    getString(R.string.quote_validity_label),
                    defaults.getValue("vigencia"),
                    InputType.TYPE_CLASS_NUMBER
                )
                addInput(
                    this,
                    "garantia",
                    getString(R.string.warranty_days_label),
                    defaults.getValue("garantia"),
                    InputType.TYPE_CLASS_NUMBER
                )
                addInput(
                    this,
                    "seguimiento_autorizacion",
                    getString(R.string.authorization_reminder_label),
                    defaults.getValue("seguimiento_autorizacion"),
                    InputType.TYPE_CLASS_NUMBER
                )
                addInput(
                    this,
                    "refaccion_atrasada",
                    getString(R.string.parts_reminder_label),
                    defaults.getValue("refaccion_atrasada"),
                    InputType.TYPE_CLASS_NUMBER
                )
                addInput(
                    this,
                    "equipo_listo",
                    getString(R.string.ready_reminder_label),
                    defaults.getValue("equipo_listo"),
                    InputType.TYPE_CLASS_NUMBER
                )
            }
        )
    }

    private fun showReview(configuration: ConfiguracionTaller) {
        render(
            title = getString(R.string.review_title),
            subtitle = getString(R.string.review_subtitle),
            step = "PASO 4 DE 4",
            primary = getString(R.string.save_label),
            secondary = getString(R.string.back_label),
            onSecondary = { showWorkshop() },
            onPrimary = { saveInitialConfiguration(configuration) },
            content = {
                addCard(this, "Taller", listOfNotNull(
                    configuration.nombre?.takeIf { it.isNotBlank() },
                    configuration.telefono?.takeIf { it.isNotBlank() },
                    configuration.direccion?.takeIf { it.isNotBlank() }
                ).ifEmpty { listOf("Sin datos de contacto") }.joinToString("\n"))
                addSpace(this, 10)
                addCard(
                    this,
                    "Moneda, zona horaria y folios",
                    "${configuration.monedaCodigo} · ${configuration.zonaHoraria}\n" +
                        "Folio: ${configuration.prefijoFolio}-${configuration.siguienteFolio}"
                )
                addSpace(this, 10)
                val fractionDigits = Currency.getInstance(configuration.monedaCodigo)
                    .defaultFractionDigits.coerceAtLeast(0)
                val charge = BigDecimal.valueOf(configuration.cargoDiagnosticoMinor, fractionDigits)
                val percent = BigDecimal(configuration.impuestoTasa).movePointRight(2).stripTrailingZeros()
                addCard(
                    this,
                    "Condiciones iniciales",
                    "Impuesto: $percent%\n" +
                        "Cargo de diagnóstico: $charge ${configuration.monedaCodigo}\n" +
                        "Vigencia: ${configuration.diasVigenciaCotizacion} días\n" +
                        "Garantía predeterminada: ${configuration.diasGarantiaPredeterminados} días\n" +
                        "Recordatorios (autorización / refacción / equipo listo): " +
                        "${configuration.diasRecordatorioSeguimientoAutorizacion} / " +
                        "${configuration.diasRecordatorioRefaccionAtrasada} / " +
                        "${configuration.diasRecordatorioEquipoListo} días"
                )
                addSpace(this, 10)
                addText(this, "Método de acceso: ${methodLabel(selectedMethod)}", color = MUTED)
            }
        )
    }

    private fun saveInitialConfiguration(configuration: ConfiguracionTaller) {
        val access = pendingAccess
        if (access == null) {
            showMessage("Vuelve a elegir y confirmar un método de acceso.")
            showAccessMethod()
            return
        }
        render(
            title = getString(R.string.saving_label),
            subtitle = "Guardando los datos de forma local y segura.",
            step = "PASO 4 DE 4",
            primary = null,
            content = {}
        )
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.guardar(configuration, access) }
                pendingAccess = null
                returnToLogin()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                showReview(configuration)
                showMessage(
                    exception.localizedMessage ?: "No se pudo guardar la configuración. Inténtalo de nuevo."
                )
            }
        }
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

    private fun accessConfiguration(
        method: String,
        derived: VerificadorDerivado? = null
    ): ConfiguracionAccesoLocal =
        ConfiguracionAccesoLocal(
            metodo = method,
            verificadorContrasena = derived?.verificador,
            salContrasena = derived?.sal,
            algoritmoContrasena = derived?.algoritmo,
            tiempoBloqueoSegundos = 300,
            actualizadoEn = Instant.now().toString()
        )

    private fun defaultFormValues(): Map<String, String> {
        val defaultCurrency = defaultCurrencyCode()
        return mapOf(
            "nombre" to "",
            "telefono" to "",
            "direccion" to "",
            "moneda" to defaultCurrency,
            "zona_horaria" to TimeZone.getDefault().id,
            "prefijo_folio" to "TC",
            "siguiente_folio" to "1",
            "impuesto" to "0",
            "cargo_diagnostico" to "0",
            "vigencia" to "7",
            "garantia" to "30",
            "seguimiento_autorizacion" to "3",
            "refaccion_atrasada" to "3",
            "equipo_listo" to "2"
        )
    }

    private fun defaultCurrencyCode(): String {
        val locale = Locale.getDefault(Locale.Category.FORMAT)
        val configuredCurrency = runCatching { Currency.getInstance(locale).currencyCode }.getOrNull()
        val availableCodes = Currency.getAvailableCurrencies().mapTo(mutableSetOf()) { it.currencyCode }
        return configuredCurrency?.takeIf { it in availableCodes }
            ?: Currency.getAvailableCurrencies()
                .asSequence()
                .filter { it.currencyCode != "XXX" && it.currencyCode != "XTS" }
                .sortedBy { it.currencyCode }
                .firstOrNull()
                ?.currencyCode
            ?: "USD"
    }

    private fun populateForm(configuration: ConfiguracionTaller) {
        val digits = Currency.getInstance(configuration.monedaCodigo).defaultFractionDigits
            .takeIf { it >= 0 } ?: 2
        fieldValues.putAll(
            mapOf(
                "nombre" to configuration.nombre.orEmpty(),
                "telefono" to configuration.telefono.orEmpty(),
                "direccion" to configuration.direccion.orEmpty(),
                "moneda" to configuration.monedaCodigo,
                "zona_horaria" to configuration.zonaHoraria,
                "prefijo_folio" to configuration.prefijoFolio,
                "siguiente_folio" to configuration.siguienteFolio.toString(),
                "impuesto" to BigDecimal(configuration.impuestoTasa)
                    .movePointRight(2).stripTrailingZeros().toPlainString(),
                "cargo_diagnostico" to BigDecimal.valueOf(
                    configuration.cargoDiagnosticoMinor,
                    digits
                ).stripTrailingZeros().toPlainString(),
                "vigencia" to configuration.diasVigenciaCotizacion.toString(),
                "garantia" to configuration.diasGarantiaPredeterminados.toString(),
                "seguimiento_autorizacion" to configuration.diasRecordatorioSeguimientoAutorizacion.toString(),
                "refaccion_atrasada" to configuration.diasRecordatorioRefaccionAtrasada.toString(),
                "equipo_listo" to configuration.diasRecordatorioEquipoListo.toString()
            )
        )
    }

    private fun datosTaller(): DatosTaller {
        fun value(name: String) = fieldValues[name] ?: ""
        return DatosTaller(
            nombre = value("nombre"),
            telefono = value("telefono"),
            direccion = value("direccion"),
            monedaCodigo = value("moneda"),
            zonaHoraria = value("zona_horaria"),
            prefijoFolio = value("prefijo_folio"),
            siguienteFolio = value("siguiente_folio"),
            impuestoPorcentaje = value("impuesto"),
            cargoDiagnostico = value("cargo_diagnostico"),
            diasVigenciaCotizacion = value("vigencia"),
            diasGarantia = value("garantia"),
            diasSeguimientoAutorizacion = value("seguimiento_autorizacion"),
            diasRefaccionAtrasada = value("refaccion_atrasada"),
            diasEquipoListo = value("equipo_listo")
        )
    }

    private fun captureCurrentFields() {
        editFields.forEach { (key, value) ->
            if (key in SELECTION_FIELD_KEYS) {
                val selectedValue = value.tag as? String
                if (selectedValue == null) {
                    fieldValues.remove(key)
                } else {
                    fieldValues[key] = selectedValue
                }
            } else if (key !in SENSITIVE_FIELD_KEYS) {
                fieldValues[key] = value.text?.toString().orEmpty()
            }
        }
    }

    companion object {
        private const val TAG = "TallerCelulares"
        private val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
        private const val STATE_METHOD = "setup.method"
        private const val FIELD_STATE_PREFIX = "setup.field."
        private val SELECTION_FIELD_KEYS = setOf("moneda", "zona_horaria")
        private val SENSITIVE_FIELD_KEYS = setOf("password", "password_confirm", "login_password")
    }

}
