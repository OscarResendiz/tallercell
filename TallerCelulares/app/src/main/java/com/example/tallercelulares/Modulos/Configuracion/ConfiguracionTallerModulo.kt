package com.example.tallercelulares.Modulos.Configuracion

import android.graphics.Color
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.example.tallercelulares.DataBase.Entidades.OpcionMetodoPago.OpcionMetodoPago
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

internal class ConfiguracionTallerModulo(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val alVolverAlTablero: () -> Unit,
    private val alBloquear: () -> Unit
) {
    private companion object {
        const val PAGE_SIZE = 500
        val TEAL = Color.rgb(0, 117, 112)
        val DARK = Color.rgb(20, 32, 48)
        val MUTED = Color.rgb(91, 105, 122)
        val ERROR = Color.rgb(176, 42, 55)
        val ALERT = Color.rgb(176, 112, 32)
    }

    private val callbackAtras = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = volverAlTablero()
    }
    private var tarea: Job? = null
    private var cerrada = false

    init {
        activity.onBackPressedDispatcher.addCallback(activity, callbackAtras)
        mostrarConfiguracion()
    }

    fun cerrar() {
        cerrada = true
        tarea?.cancel()
        callbackAtras.isEnabled = false
    }

    private fun volverAlTablero() {
        cerrar()
        alVolverAlTablero()
    }

    private fun mostrarConfiguracion() {
        val contenido = crearContenido("Configuración del taller")
        contenido.addView(texto("Cargando configuración…", 15f, MUTED, false))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                val monedas = database.catalogoMonedaSistemaDao().obtenerActivas()
                val zonas = database.catalogoZonaHorariaSistemaDao().obtenerActivas()
                val metodos = database.opcionMetodoPagoDao().obtenerTodas()
                if (cerrada) return@launch
                contenido.removeAllViews()

                if (configuracion == null) {
                    contenido.addView(texto(
                        "No existe una configuración del taller. Completa la configuración inicial antes de guardar parámetros comerciales.",
                        16f,
                        ALERT,
                        false
                    ), margenInferior())
                    contenido.addView(boton("Volver al tablero", "Volver") { volverAlTablero() })
                    return@launch
                }

                val campos = mutableMapOf<String, TextInputEditText>()
                val layouts = mutableMapOf<String, TextInputLayout>()

                contenido.addView(texto("Datos generales", 18f, DARK, true), margenInferior())
                agregarCampo(contenido, layouts, campos, "nombre", "Nombre del taller",
                    configuracion.nombre.orEmpty())
                agregarCampo(contenido, layouts, campos, "telefono", "Teléfono",
                    configuracion.telefono.orEmpty(), InputType.TYPE_CLASS_PHONE)
                agregarCampo(contenido, layouts, campos, "direccion", "Dirección",
                    configuracion.direccion.orEmpty(), multilinea = true)

                contenido.addView(texto("Región y folios", 18f, DARK, true), margenInferior())
                val monedaSpinner = agregarSpinner(
                    contenido,
                    "Moneda",
                    monedas.map { "${it.codigo} — ${it.nombre}" },
                    monedas.indexOfFirst { it.codigo == configuracion.monedaCodigo }.takeIf { it >= 0 } ?: 0
                )
                val zonaSpinner = agregarSpinner(
                    contenido,
                    "Zona horaria",
                    zonas.map { "${it.id} — ${it.nombre}" },
                    zonas.indexOfFirst { it.id == configuracion.zonaHoraria }.takeIf { it >= 0 } ?: 0
                )
                agregarCampo(contenido, layouts, campos, "prefijo_folio", "Prefijo de folio",
                    configuracion.prefijoFolio)
                agregarCampo(contenido, layouts, campos, "siguiente_folio", "Siguiente folio",
                    configuracion.siguienteFolio.toString(), InputType.TYPE_CLASS_NUMBER)

                contenido.addView(texto("Condiciones comerciales", 18f, DARK, true), margenInferior())
                val impuesto = configuracion.impuestoTasa.toBigDecimalOrNull()
                    ?.movePointRight(2)?.stripTrailingZeros()?.toPlainString().orEmpty()
                agregarCampo(contenido, layouts, campos, "impuesto", "Impuesto (porcentaje)",
                    impuesto, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                val cargo = BigDecimalMenor.formato(
                    configuracion.cargoDiagnosticoMinor,
                    configuracion.monedaCodigo
                )
                agregarCampo(contenido, layouts, campos, "cargo_diagnostico",
                    "Cargo de diagnóstico", cargo,
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
                agregarCampo(contenido, layouts, campos, "vigencia",
                    "Vigencia de cotización (días)",
                    configuracion.diasVigenciaCotizacion.toString(), InputType.TYPE_CLASS_NUMBER)
                agregarCampo(contenido, layouts, campos, "garantia",
                    "Garantía predeterminada (días)",
                    configuracion.diasGarantiaPredeterminados.toString(), InputType.TYPE_CLASS_NUMBER)
                agregarCampo(contenido, layouts, campos, "seguimiento",
                    "Seguimiento de autorización (días)",
                    configuracion.diasRecordatorioSeguimientoAutorizacion.toString(),
                    InputType.TYPE_CLASS_NUMBER)
                agregarCampo(contenido, layouts, campos, "refaccion_atrasada",
                    "Recordatorio de refacción atrasada (días)",
                    configuracion.diasRecordatorioRefaccionAtrasada.toString(),
                    InputType.TYPE_CLASS_NUMBER)
                agregarCampo(contenido, layouts, campos, "equipo_listo",
                    "Recordatorio de equipo listo (días)",
                    configuracion.diasRecordatorioEquipoListo.toString(),
                    InputType.TYPE_CLASS_NUMBER)

                contenido.addView(texto("Textos documentales", 18f, DARK, true), margenInferior())
                agregarCampo(contenido, layouts, campos, "texto_recepcion",
                    "Texto de recepción", configuracion.textoRecepcion.orEmpty(),
                    multilinea = true)
                agregarCampo(contenido, layouts, campos, "texto_autorizacion",
                    "Texto de autorización", configuracion.textoAutorizacion.orEmpty(),
                    multilinea = true)
                agregarCampo(contenido, layouts, campos, "texto_garantia",
                    "Texto de garantía", configuracion.textoGarantia.orEmpty(),
                    multilinea = true)

                val guardar = boton("Guardar configuración", "Guardar cambios") {
                    guardarConfiguracion(
                        monedaSpinner,
                        zonaSpinner,
                        campos,
                        layouts,
                        metodos
                    )
                }
                contenido.addView(guardar, margenInferior())

                contenido.addView(texto("Métodos de pago", 18f, DARK, true), margenInferior())
                contenido.addView(texto(
                    "Solo opciones activas estarán disponibles para movimientos de caja. No captures tarjeta, CVV, PIN ni credenciales.",
                    14f,
                    MUTED,
                    false
                ), margenInferior())

                if (metodos.isEmpty()) {
                    contenido.addView(texto(
                        "No hay métodos configurados. Agrega opciones autorizadas por el taller.",
                        15f,
                        ALERT,
                        false
                    ), margenInferior())
                } else {
                    metodos.forEach { metodo ->
                        val fila = LinearLayout(activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
                        }
                        fila.addView(
                            texto(
                                "${metodo.nombre}\n${if (metodo.activo) "Activo" else "Inactivo"}",
                                15f,
                                if (metodo.activo) DARK else MUTED,
                                false
                            ),
                            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        )
                        fila.addView(boton(
                            if (metodo.activo) "Desactivar" else "Activar",
                            "${if (metodo.activo) "Desactivar" else "Activar"} ${metodo.nombre}"
                        ) { cambiarEstadoMetodo(metodo) })
                        contenido.addView(fila, margenInferior())
                    }
                }
                contenido.addView(boton("Agregar método de pago", "Agregar opción de pago") {
                    solicitarNuevoMetodo()
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (cerrada) return@launch
                contenido.removeAllViews()
                contenido.addView(texto(
                    "No se pudo cargar la configuración. Reintenta desde el tablero.",
                    15f,
                    ERROR,
                    false
                ), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a cargar configuración") {
                    mostrarConfiguracion()
                })
            }
        }
    }

    private fun guardarConfiguracion(
        monedaSpinner: Spinner,
        zonaSpinner: Spinner,
        campos: Map<String, TextInputEditText>,
        layouts: Map<String, TextInputLayout>,
        metodos: List<OpcionMetodoPago>
    ) {
        layouts.values.forEach { it.error = null }
        val moneda = (monedaSpinner.selectedItem as? String)
            ?.substringBefore(" —")?.trim().orEmpty()
        val zona = (zonaSpinner.selectedItem as? String)
            ?.substringBefore(" —")?.trim().orEmpty()
        val camposTaller = CamposConfiguracionTaller(
            nombre = campos["nombre"]?.text?.toString().orEmpty(),
            telefono = campos["telefono"]?.text?.toString().orEmpty(),
            direccion = campos["direccion"]?.text?.toString().orEmpty(),
            monedaCodigo = moneda,
            zonaHoraria = zona,
            prefijoFolio = campos["prefijo_folio"]?.text?.toString().orEmpty(),
            siguienteFolio = campos["siguiente_folio"]?.text?.toString().orEmpty(),
            impuestoPorcentaje = campos["impuesto"]?.text?.toString().orEmpty(),
            cargoDiagnostico = campos["cargo_diagnostico"]?.text?.toString().orEmpty(),
            diasVigenciaCotizacion = campos["vigencia"]?.text?.toString().orEmpty(),
            diasGarantia = campos["garantia"]?.text?.toString().orEmpty(),
            diasSeguimientoAutorizacion = campos["seguimiento"]?.text?.toString().orEmpty(),
            diasRefaccionAtrasada = campos["refaccion_atrasada"]?.text?.toString().orEmpty(),
            diasEquipoListo = campos["equipo_listo"]?.text?.toString().orEmpty(),
            textoRecepcion = campos["texto_recepcion"]?.text?.toString().orEmpty(),
            textoAutorizacion = campos["texto_autorizacion"]?.text?.toString().orEmpty(),
            textoGarantia = campos["texto_garantia"]?.text?.toString().orEmpty()
        )
        val errores = ReglasConfiguracion.validar(camposTaller)
        if (errores.isNotEmpty()) {
            errores.forEach { (clave, mensaje) ->
                layouts[clave]?.error = mensaje
                if (clave == "moneda" || clave == "zona_horaria") {
                    // Los spinners muestran su propio estado; el resumen se presenta al guardar.
                    return@forEach
                }
            }
            val mensaje = errores.values.joinToString("\n")
            MaterialAlertDialogBuilder(activity)
                .setTitle("Revisa la configuración")
                .setMessage(mensaje)
                .setPositiveButton("Cerrar", null)
                .show()
            return
        }
        val valores = ReglasConfiguracion.convertir(camposTaller) ?: return

        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                val guardados = database.withTransaction {
                    val actual = database.configuracionTallerDao().obtenerPorId("TALLER")
                        ?: throw IllegalStateException("Configuración ausente")
                    database.configuracionTallerDao().actualizarCampos(
                        id = "TALLER",
                        nombre = valores.nombre,
                        telefono = valores.telefono,
                        direccion = valores.direccion,
                        monedaCodigo = valores.monedaCodigo,
                        zonaHoraria = valores.zonaHoraria,
                        prefijoFolio = valores.prefijoFolio,
                        siguienteFolio = valores.siguienteFolio,
                        impuestoTasa = valores.impuestoTasa,
                        cargoDiagnosticoMinor = valores.cargoDiagnosticoMinor,
                        diasVigenciaCotizacion = valores.diasVigenciaCotizacion,
                        diasGarantiaPredeterminados = valores.diasGarantia,
                        diasSeguimientoAutorizacion = valores.diasSeguimientoAutorizacion,
                        diasRefaccionAtrasada = valores.diasRefaccionAtrasada,
                        diasEquipoListo = valores.diasEquipoListo,
                        textoRecepcion = valores.textoRecepcion,
                        textoAutorizacion = valores.textoAutorizacion,
                        textoGarantia = valores.textoGarantia,
                        actualizadoEn = ahora
                    )
                    database.configuracionTallerDao().obtenerPorId("TALLER")
                }
                if (cerrada) return@launch
                if (guardados == null) throw IllegalStateException("No se pudo confirmar")
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Configuración guardada")
                    .setMessage("Los parámetros vigentes se aplicarán a operaciones nuevas.")
                    .setPositiveButton("Aceptar") { _, _ -> mostrarConfiguracion() }
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (cerrada) return@launch
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo guardar")
                    .setMessage("La configuración no fue modificada. Revisa los datos e intenta de nuevo.")
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun solicitarNuevoMetodo() {
        val campo = TextInputEditText(activity).apply {
            hint = "Nombre del método"
            contentDescription = "Nombre del método"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val layout = TextInputLayout(activity).apply {
            addView(campo)
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle("Agregar método de pago")
            .setView(layout)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = campo.text?.toString().orEmpty()
                val error = ReglasConfiguracion.validarMetodoPago(nombre)
                if (error != null) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Dato inválido")
                        .setMessage(error)
                        .setPositiveButton("Cerrar", null)
                        .show()
                    return@setPositiveButton
                }
                agregarMetodo(nombre.trim())
            }
            .show()
    }

    private fun agregarMetodo(nombre: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                val existente = database.opcionMetodoPagoDao().obtenerPorNombre(nombre)
                if (existente != null) {
                    throw IllegalArgumentException("Ese método ya está configurado.")
                }
                database.withTransaction {
                    database.opcionMetodoPagoDao().insertar(
                        OpcionMetodoPago(
                            nombre = nombre,
                            activo = true,
                            creadoEn = ahora,
                            actualizadoEn = ahora
                        )
                    )
                }
                if (!cerrada) mostrarConfiguracion()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (cerrada) return@launch
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo agregar")
                    .setMessage(
                        if (exception is IllegalArgumentException) exception.message
                        else "El método no fue guardado. Intenta de nuevo."
                    )
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun cambiarEstadoMetodo(metodo: OpcionMetodoPago) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                database.opcionMetodoPagoDao().actualizarEstado(
                    metodo.id,
                    !metodo.activo,
                    Instant.now().toString()
                )
                if (!cerrada) mostrarConfiguracion()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (cerrada) return@launch
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo cambiar")
                    .setMessage("El estado del método no fue modificado.")
                    .setPositiveButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun crearContenido(titulo: String): LinearLayout {
        container.removeAllViews()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(18), dp(24), dp(16))
        }
        val encabezado = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        encabezado.addView(
            texto(titulo, 24f, DARK, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        encabezado.addView(boton("Atrás", "Volver") { volverAlTablero() })
        encabezado.addView(boton("Bloquear", "Bloquear la aplicación") { cerrarYBloquear() })
        root.addView(encabezado, margenInferior())
        val scroll = ScrollView(activity).apply { isFillViewport = true }
        val contenido = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(contenido)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        container.addView(root)
        return contenido
    }

    private fun cerrarYBloquear() {
        cerrar()
        alBloquear()
    }

    private fun agregarCampo(
        contenido: LinearLayout,
        layouts: MutableMap<String, TextInputLayout>,
        campos: MutableMap<String, TextInputEditText>,
        clave: String,
        etiqueta: String,
        valor: String,
        tipo: Int = InputType.TYPE_CLASS_TEXT,
        multilinea: Boolean = false
    ) {
        val layout = TextInputLayout(activity).apply {
            hint = etiqueta
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val campo = TextInputEditText(layout.context).apply {
            inputType = tipo
            contentDescription = etiqueta
            if (multilinea) {
                minLines = 3
                maxLines = 6
                gravity = Gravity.TOP
            } else setSingleLine(true)
            setText(valor)
        }
        layout.addView(campo)
        contenido.addView(layout, margenInferior())
        layouts[clave] = layout
        campos[clave] = campo
    }

    private fun agregarSpinner(
        contenido: LinearLayout,
        etiqueta: String,
        opciones: List<String>,
        seleccionInicial: Int
    ): Spinner {
        val opcionesSeguras = opciones.ifEmpty { listOf("Sin opciones disponibles") }
        val layout = TextInputLayout(activity).apply {
            hint = etiqueta
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val spinner = Spinner(activity).apply {
            contentDescription = etiqueta
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, opcionesSeguras)
            if (seleccionInicial in opcionesSeguras.indices) setSelection(seleccionInicial)
        }
        layout.addView(spinner)
        contenido.addView(layout, margenInferior())
        return spinner
    }

    private fun boton(titulo: String, descripcion: String, accion: () -> Unit): MaterialButton =
        MaterialButton(activity).apply {
            text = titulo
            isAllCaps = false
            contentDescription = descripcion
            setOnClickListener { accion() }
        }

    private fun texto(
        valor: String,
        tamano: Float,
        color: Int,
        negrita: Boolean
    ): TextView = TextView(activity).apply {
        text = valor
        textSize = tamano
        setTextColor(color)
        if (negrita) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun margenInferior(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(12) }

    private fun dp(valor: Int): Int =
        (valor * activity.resources.displayMetrics.density).toInt()
}

private object BigDecimalMenor {
    fun formato(valorMinor: Long, monedaCodigo: String): String =
        try {
            val digitos = java.util.Currency.getInstance(monedaCodigo).defaultFractionDigits
                .takeIf { it >= 0 } ?: 2
            java.math.BigDecimal.valueOf(valorMinor, digitos).toPlainString()
        } catch (_: Exception) {
            ""
        }
}
