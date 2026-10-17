package com.example.tallercelulares

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

class MainActivity : AppCompatActivity() {
    private lateinit var repository: RepositorioConfiguracionInicial
    private lateinit var root: android.widget.FrameLayout
    private val fieldValues = linkedMapOf<String, String>()
    private val editFields = mutableMapOf<String, EditText>()
    private val fieldLayouts = mutableMapOf<String, TextInputLayout>()
    private var monedasSistema: List<CatalogoMonedaSistema> = emptyList()
    private var zonasHorariasSistema: List<CatalogoZonaHorariaSistema> = emptyList()
    private var selectedMethod = RepositorioConfiguracionInicial.METODO_CONTRASENA
    private var pendingAccess: ConfiguracionAccesoLocal? = null
    private var deviceAuthSuccess: (() -> Unit)? = null
    private var deviceAuthError: (() -> Unit)? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        repository = RepositorioConfiguracionInicial(
            TallerCelularesDatabase.obtenerInstancia(applicationContext)
        )
        savedInstanceState?.keySet()?.filter { it.startsWith(FIELD_STATE_PREFIX) }?.forEach { key ->
            val field = key.removePrefix(FIELD_STATE_PREFIX)
            if (field !in SENSITIVE_FIELD_KEYS) {
                fieldValues[field] = savedInstanceState.getString(key).orEmpty()
            }
        }
        selectedMethod = savedInstanceState?.getString(STATE_METHOD)
            ?: RepositorioConfiguracionInicial.METODO_CONTRASENA
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
                val state = withContext(Dispatchers.IO) { repository.estado() }
                when (state) {
                    EstadoConfiguracionInicial.COMPLETA -> showLogin()
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
                            EstadoConfiguracionInicial.COMPLETA -> showLogin()
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
                val methodGroup = RadioGroup(this@MainActivity).apply {
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
                showLogin()
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
                showWelcome(incomplete = true)
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

    private fun addInput(
        container: LinearLayout,
        key: String,
        label: String,
        default: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        password: Boolean = false
    ) {
        val layout = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColor(TEAL)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            if (password) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        val editText = TextInputEditText(layout.context).apply {
            this.inputType = inputType
            setText(fieldValues[key] ?: default)
            setTextColor(DARK)
            textSize = 16f
            if (password) {
                transformationMethod = PasswordTransformationMethod.getInstance()
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
                isLongClickable = false
            }
            if (key == "nombre" || key == "direccion") {
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
            }
        }
        layout.addView(editText)
        container.addView(layout)
        editFields[key] = editText
        fieldLayouts[key] = layout
    }

    private fun addSelectionDropdown(
        container: LinearLayout,
        key: String,
        label: String,
        selectedValue: String,
        options: List<SelectionOption>
    ) {
        val selectedOption = options.firstOrNull { it.value == fieldValues[key] }
            ?: options.firstOrNull { it.value == selectedValue }
            ?: options.firstOrNull()
        val layout = TextInputLayout(this).apply {
            hint = label
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxStrokeColor(TEAL)
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            setEndIconDrawable(android.R.drawable.arrow_down_float)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        val selectedText = TextInputEditText(this).apply {
            setText(selectedOption?.label.orEmpty())
            tag = selectedOption?.value
            setTextColor(DARK)
            textSize = 16f
            isFocusable = false
            isClickable = true
            showSoftInputOnFocus = false
            contentDescription = "$label: ${selectedOption?.label.orEmpty()}. Toca para elegir otra opción."
            setOnClickListener { showSelectionDialog(layout, key, label, options, this) }
        }
        layout.addView(selectedText)
        layout.setEndIconOnClickListener {
            showSelectionDialog(layout, key, label, options, selectedText)
        }
        selectedOption?.let { fieldValues[key] = it.value }
        container.addView(layout)
        editFields[key] = selectedText
        fieldLayouts[key] = layout
    }

    private fun showSelectionDialog(
        fieldLayout: TextInputLayout,
        fieldKey: String,
        title: String,
        options: List<SelectionOption>,
        selectedText: TextInputEditText
    ) {
        if (options.isEmpty()) {
            fieldLayout.error = "No hay opciones disponibles en el catálogo local."
            return
        }

        val searchView = SearchView(this).apply {
            queryHint = "Buscar en ${options.size} opciones"
            isIconified = false
            clearFocus()
        }
        val listView = ListView(this).apply {
            dividerHeight = dp(1)
            setPadding(0, dp(4), 0, dp(4))
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            options.toMutableList()
        )
        listView.adapter = adapter

        val dialogContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), dp(8))
            addView(
                searchView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                listView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(420)
                )
            )
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogContent)
            .setNegativeButton("Cerrar", null)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val option = adapter.getItem(position)
            if (option != null) {
                selectedText.setText(option.label)
                selectedText.contentDescription =
                    "$title: ${option.label}. Toca para elegir otra opción."
                selectedText.tag = option.value
                fieldValues[fieldKey] = option.value
                fieldLayout.error = null
                dialog.dismiss()
            }
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter.filter(newText.orEmpty())
                return true
            }
        })
        dialog.setOnShowListener {
            val selectedIndex = options.indexOfFirst { it.value == fieldValues[fieldKey] }
            if (selectedIndex >= 0) listView.setSelection(selectedIndex)
        }
        dialog.show()
    }

    private fun render(
        title: String,
        subtitle: String,
        step: String?,
        primary: String?,
        secondary: String? = null,
        onPrimary: (() -> Unit)? = null,
        onSecondary: (() -> Unit)? = null,
        content: LinearLayout.() -> Unit
    ) {
        root.removeAllViews()
        editFields.clear()
        fieldLayouts.clear()
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(16))
        }
        addText(header, getString(R.string.brand_name), color = TEAL, size = 12f, bold = true)
        addSpace(header, 8)
        addText(header, title, color = DARK, size = 26f, bold = true)
        addSpace(header, 6)
        addText(header, subtitle, color = MUTED, size = 15f)
        step?.let {
            addSpace(header, 12)
            addText(header, it, color = TEAL, size = 12f, bold = true)
        }
        screen.addView(header)

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(16))
        }
        body.content()
        scrollView.addView(body)
        screen.addView(
            scrollView,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        if (primary != null || secondary != null) {
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, dp(4))
            }
            if (secondary != null) {
                actions.addView(
                    MaterialButton(this).apply {
                        text = secondary
                        isAllCaps = false
                        setTextColor(TEAL)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(Color.TRANSPARENT)
                        setOnClickListener { onSecondary?.invoke() }
                    },
                    LinearLayout.LayoutParams(0, dp(52), 1f)
                )
            }
            if (primary != null) {
                actions.addView(
                    MaterialButton(this).apply {
                        text = primary
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        backgroundTintList = android.content.res.ColorStateList.valueOf(TEAL)
                        cornerRadius = dp(14)
                        setOnClickListener { onPrimary?.invoke() }
                    },
                    LinearLayout.LayoutParams(
                        0,
                        dp(52),
                        if (secondary == null) 1f else 1.4f
                    )
                )
            }
            screen.addView(actions)
        }
        root.addView(screen)
    }

    private fun addCard(container: LinearLayout, title: String, body: String) {
        val card = MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.rgb(229, 233, 239)
            strokeWidth = dp(1)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        addText(content, title, color = DARK, size = 16f, bold = true)
        addSpace(content, 6)
        addText(content, body, color = MUTED, size = 14f)
        card.addView(content)
        container.addView(card)
    }

    private fun addSectionTitle(container: LinearLayout, title: String) {
        val text = TextView(this).apply {
            this.text = title
            textSize = 17f
            setTextColor(DARK)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(10), 0, dp(10))
        }
        container.addView(text)
    }

    private fun addText(
        container: LinearLayout,
        value: String,
        color: Int,
        size: Float = 14f,
        bold: Boolean = false
    ) {
        val text = TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }
        container.addView(text)
    }

    private fun addSpace(container: LinearLayout, height: Int) {
        container.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun showMessage(message: String) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun methodLabel(method: String): String = when (method) {
        RepositorioConfiguracionInicial.METODO_CONTRASENA -> getString(R.string.password_option)
        RepositorioConfiguracionInicial.METODO_CREDENCIAL_ANDROID -> getString(R.string.device_option)
        RepositorioConfiguracionInicial.METODO_BIOMETRIA_ANDROID -> getString(R.string.biometric_option)
        else -> "Sin método"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "TallerCelulares"
        private val BIOMETRIC_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK
        private const val STATE_METHOD = "setup.method"
        private const val FIELD_STATE_PREFIX = "setup.field."
        private val SELECTION_FIELD_KEYS = setOf("moneda", "zona_horaria")
        private val SENSITIVE_FIELD_KEYS = setOf("password", "password_confirm", "login_password")
        private val DARK = Color.rgb(20, 32, 48)
        private val MUTED = Color.rgb(91, 105, 122)
        private val TEAL = Color.rgb(0, 117, 112)
        private val ERROR = Color.rgb(176, 42, 55)
    }

    private data class SelectionOption(val value: String, val label: String) {
        override fun toString(): String = label
    }
}
