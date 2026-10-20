package com.example.tallercelulares.Modulos.tablero

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenTableroRegistro
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTaller
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Vista del tablero alojada en AccesoActivity para conservar su guard de sesión e inactividad.
 */
internal class TableroPrincipal(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val alBloquear: () -> Unit
) {
    private data class FiltroEstado(val codigo: String?, val etiqueta: String) {
        override fun toString(): String = etiqueta
    }

    private val handler = Handler(Looper.getMainLooper())
    private val filtrosConocidos = listOf(
        "RECIBIDA", "COLA_DIAGNOSTICO", "EN_DIAGNOSTICO", "ESPERANDO_AUTORIZACION",
        "ESPERANDO_REFACCION", "COLA_REPARACION", "EN_REPARACION", "CONTROL_CALIDAD",
        "LISTA_ENTREGA", "DEVOLUCION_SIN_REPARAR", "ENTREGADA", "CANCELADA"
    )
    private val conteos = mutableMapOf<String, Int>()
    private val ordenesCargadas = mutableListOf<TarjetaOrdenTablero>()
    private var codigosEstado = filtrosConocidos
    private var estadoSeleccionado: String? = null
    private var grupoSeleccionado: GrupoTablero? = null
    private var taller: ConfiguracionTaller? = null
    private var zonaHoraria: ZoneId = ZoneId.of("UTC")
    private var desplazamiento = 0
    private var hayMas = false
    private var cargando = false
    private var cambiandoFiltro = false
    private var tareaCarga: Job? = null
    private var filtroActualizado = false

    private lateinit var tituloTaller: TextView
    private lateinit var textoFiltro: TextView
    private lateinit var textoEstado: TextView
    private lateinit var listaOrdenes: LinearLayout
    private lateinit var resumen: LinearLayout
    private lateinit var selectorEstado: Spinner
    private lateinit var busqueda: TextInputEditText
    private lateinit var botonLimpiarFiltro: MaterialButton
    private lateinit var botonReintentar: MaterialButton
    private lateinit var botonMas: MaterialButton

    private val busquedaPendiente = Runnable { actualizarTablero(reiniciar = true) }

    init {
        construirInterfaz()
        actualizarTablero(reiniciar = true)
    }

    fun refrescar() {
        if (!cargando) actualizarTablero(reiniciar = true)
    }

    private fun construirInterfaz() {
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            contentDescription = "Tablero principal del taller"
        }
        val contenido = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(24))
        }
        scroll.addView(
            contenido,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        container.removeAllViews()
        container.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val encabezado = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val titulo = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        tituloTaller = texto("Taller Celulares", 18f, Color.rgb(0, 117, 112), true)
        titulo.addView(tituloTaller)
        titulo.addView(texto("Tablero de órdenes", 25f, COLOR_TEXTO, true))
        encabezado.addView(
            titulo,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        val menuButton = MaterialButton(activity).apply {
            text = "Áreas"
            contentDescription = "Ver áreas de trabajo y su disponibilidad"
            setOnClickListener { mostrarMenu() }
        }
        encabezado.addView(menuButton)
        contenido.addView(encabezado)

        val bloquearButton = MaterialButton(activity).apply {
            text = "Bloquear aplicación"
            contentDescription = "Bloquear la aplicación"
            setOnClickListener { alBloquear() }
        }
        contenido.addView(bloquearButton, margenInferior())

        contenido.addView(texto("Resumen de órdenes activas", 18f, COLOR_TEXTO, true))
        val resumenScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            contentDescription = "Resumen por etapa; toca una etapa para filtrar"
        }
        resumen = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        resumenScroll.addView(resumen)
        contenido.addView(resumenScroll)

        contenido.addView(texto("Buscar órdenes", 18f, COLOR_TEXTO, true))
        val campoBusqueda = TextInputLayout(activity).apply {
            hint = "Folio, cliente, teléfono o equipo"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            contentDescription = "Buscar por folio, cliente, teléfono, marca, modelo o serie"
        }
        busqueda = TextInputEditText(campoBusqueda.context).apply {
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            contentDescription = "Buscar por folio, cliente, teléfono, marca, modelo o serie"
        }
        campoBusqueda.addView(busqueda)
        contenido.addView(campoBusqueda, margenInferior())
        busqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarEtiquetaFiltro()
                handler.removeCallbacks(busquedaPendiente)
                handler.postDelayed(busquedaPendiente, DEBOUNCE_BUSQUEDA_MILLIS)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        contenido.addView(texto("Filtrar por estado", 16f, COLOR_TEXTO, true))
        selectorEstado = Spinner(activity).apply {
            contentDescription = "Filtrar órdenes por estado"
        }
        contenido.addView(selectorEstado, margenInferior())
        configurarEstados(filtrosConocidos)
        selectorEstado.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (cambiandoFiltro) return
                    val selected = selectorEstado.selectedItem as? FiltroEstado ?: return
                    if (filtroActualizado && estadoSeleccionado != selected.codigo) {
                        estadoSeleccionado = selected.codigo
                        grupoSeleccionado = null
                        actualizarTablero(reiniciar = true)
                    }
                    filtroActualizado = true
                }
            }

        val filaFiltro = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        textoFiltro = texto("Sin filtros aplicados", 14f, COLOR_SECUNDARIO, false)
        filaFiltro.addView(textoFiltro, LinearLayout.LayoutParams(0, -2, 1f))
        botonLimpiarFiltro = MaterialButton(activity).apply {
            text = "Limpiar"
            contentDescription = "Limpiar los filtros aplicados"
            visibility = View.GONE
            setOnClickListener { limpiarFiltros() }
        }
        filaFiltro.addView(botonLimpiarFiltro)
        contenido.addView(filaFiltro)

        textoEstado = texto("Cargando órdenes…", 15f, COLOR_SECUNDARIO, false)
        contenido.addView(textoEstado)
        botonReintentar = MaterialButton(activity).apply {
            text = "Reintentar"
            visibility = View.GONE
            setOnClickListener { actualizarTablero(reiniciar = true) }
        }
        contenido.addView(botonReintentar)
        listaOrdenes = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(listaOrdenes)

        botonMas = MaterialButton(activity).apply {
            text = "Cargar más órdenes"
            visibility = View.GONE
            setOnClickListener { cargarSiguientePagina() }
        }
        contenido.addView(botonMas)

        val notaModulos = MaterialCardView(activity).apply {
            radius = dp(12).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.rgb(242, 246, 248))
        }
        val nota = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        nota.addView(texto("Módulos en preparación", 15f, COLOR_TEXTO, true))
        nota.addView(
            texto(
                "Las demás áreas se muestran como pendientes; todavía no realizan operaciones.",
                14f,
                COLOR_SECUNDARIO,
                false
            )
        )
        notaModulos.addView(nota)
        contenido.addView(notaModulos, margenSuperior())
    }

    private fun actualizarTablero(reiniciar: Boolean) {
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            ordenesCargadas.clear()
            resumen.removeAllViews()
            listaOrdenes.removeAllViews()
            botonMas.visibility = View.GONE
            textoEstado.text = "Cargando órdenes…"
            botonReintentar.visibility = View.GONE
        }
        tareaCarga?.cancel()
        val filtro = busqueda.text?.toString().orEmpty()
        val estado = estadoSeleccionado
        val grupo = grupoSeleccionado
        val offsetSolicitado = desplazamiento
        cargando = true
        textoEstado.visibility = View.VISIBLE
        botonMas.isEnabled = false
        tareaCarga = activity.lifecycleScope.launch {
            try {
                val databaseState = if (offsetSolicitado == 0) {
                    val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                        ?: error("No se encontró la configuración del taller.")
                    val zone = ZoneId.of(configuracion.zonaHoraria)
                    val counts = database.ordenServicioDao().obtenerConteosTablero()
                    Triple(configuracion, zone, counts)
                } else {
                    Triple(
                        requireNotNull(taller),
                        zonaHoraria,
                        emptyList()
                    )
                }
                if (offsetSolicitado == 0) {
                    taller = databaseState.first
                    zonaHoraria = databaseState.second
                    conteos.clear()
                    databaseState.third.forEach { conteos[it.estado] = it.cantidad }
                    codigosEstado = (filtrosConocidos + conteos.keys)
                        .distinct()
                        .sortedBy { filtrosConocidos.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }
                    configurarEstados(codigosEstado)
                    tituloTaller.text = databaseState.first.nombre?.takeIf(String::isNotBlank)
                        ?: "Taller Celulares"
                    mostrarResumen()
                }

                val ahora = Instant.now()
                val page = consultarPagina(
                    consulta = filtro,
                    estado = estado,
                    grupo = grupo,
                    ahora = ahora,
                    offset = offsetSolicitado
                )
                val registros = page.take(PAGE_SIZE)
                hayMas = page.size > PAGE_SIZE
                val configuracion = requireNotNull(taller)
                val tarjetas = registros.map {
                    ReglasTablero.crearTarjeta(
                        orden = it,
                        ahora = ahora,
                        zonaHoraria = zonaHoraria,
                        diasSeguimientoAutorizacion =
                            configuracion.diasRecordatorioSeguimientoAutorizacion,
                        diasRefaccionAtrasada = configuracion.diasRecordatorioRefaccionAtrasada,
                        diasEquipoListo = configuracion.diasRecordatorioEquipoListo
                    )
                }
                if (offsetSolicitado == 0) {
                    mostrarEstadoSinResultados(tarjetas.isEmpty(), filtro)
                } else {
                    textoEstado.text = ""
                    textoEstado.visibility = View.GONE
                }
                tarjetas.forEach { agregarTarjeta(it) }
                ordenesCargadas.addAll(tarjetas)
                desplazamiento += registros.size
                botonMas.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonMas.isEnabled = true
                botonReintentar.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                textoEstado.text =
                    "No se pudieron consultar las órdenes. Tus datos no se modificaron."
                textoEstado.visibility = View.VISIBLE
                botonReintentar.visibility = View.VISIBLE
                botonMas.visibility = if (ordenesCargadas.isNotEmpty()) View.VISIBLE else View.GONE
                botonMas.isEnabled = true
            } finally {
                cargando = false
            }
        }
    }

    private suspend fun consultarPagina(
        consulta: String,
        estado: String?,
        grupo: GrupoTablero?,
        ahora: Instant,
        offset: Int
    ): List<OrdenTableroRegistro> {
        val argumentos = mutableListOf<Any>()
        val condiciones = mutableListOf("o.eliminada_en IS NULL")
        if (estado != null) {
            condiciones += "o.estado = ?"
            argumentos += estado
        } else if (grupo != null) {
            condiciones += "o.estado IN (${grupo.estados.joinToString(",") { "?" }})"
            argumentos.addAll(grupo.estados)
        } else {
            condiciones += "o.estado NOT IN ('ENTREGADA', 'CANCELADA')"
        }

        val consultaNormalizada = ReglasTablero.normalizarBusqueda(consulta)
        if (consultaNormalizada.isNotEmpty()) {
            val camposTexto = listOf(
                "o.folio",
                "o.cliente_nombre_snapshot",
                "o.tipo_equipo_snapshot",
                "o.marca_snapshot",
                "o.modelo_snapshot",
                "o.serie_snapshot",
                "o.imei_snapshot"
            )
            val camposCoincidentes = camposTexto.map { campo ->
                "${expresionNormalizada(campo)} LIKE ?"
            }.toMutableList()
            val patron = "%$consultaNormalizada%"
            repeat(camposTexto.size) { argumentos += patron }

            val soloDigitos = consulta.filter(Char::isDigit)
            if (soloDigitos.length >= 3) {
                camposCoincidentes += "${expresionDigitos("o.cliente_contacto_snapshot")} LIKE ?"
                argumentos += "%$soloDigitos%"
                camposCoincidentes += "${expresionDigitos("o.serie_snapshot")} LIKE ?"
                argumentos += "%$soloDigitos%"
                camposCoincidentes += "${expresionDigitos("o.imei_snapshot")} LIKE ?"
                argumentos += "%$soloDigitos%"
            } else {
                camposCoincidentes += "${expresionNormalizada("o.cliente_contacto_snapshot")} LIKE ?"
                argumentos += patron
            }
            condiciones += "(${camposCoincidentes.joinToString(" OR ")})"
        }

        argumentos += ahora.toString()
        argumentos += PAGE_SIZE + 1
        argumentos += offset
        val sql = """
            SELECT o.id, o.folio, o.cliente_nombre_snapshot, o.cliente_contacto_snapshot,
                   o.tipo_equipo_snapshot, o.marca_snapshot, o.modelo_snapshot,
                   o.serie_snapshot, o.imei_snapshot, o.estado, o.recibida_en,
                   o.fecha_prometida,
                   COALESCE(
                       (
                           SELECT e.registrado_en
                           FROM evento_orden e
                           WHERE e.orden_id = o.id AND e.estado_nuevo = o.estado
                           ORDER BY e.registrado_en DESC
                           LIMIT 1
                       ),
                       o.recibida_en
                   ) AS estado_desde
            FROM orden_servicio o
            WHERE ${condiciones.joinToString(" AND ")}
            ORDER BY
                CASE
                    WHEN o.fecha_prometida IS NOT NULL
                         AND o.fecha_prometida < ?
                         AND o.estado NOT IN ('ENTREGADA', 'CANCELADA') THEN 0
                    WHEN o.estado IN ('ESPERANDO_AUTORIZACION', 'ESPERANDO_REFACCION', 'LISTA_ENTREGA') THEN 1
                    ELSE 2
                END,
                o.recibida_en ASC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return database.ordenServicioDao().consultarTablero(
            SimpleSQLiteQuery(sql, argumentos.toTypedArray())
        )
    }

    private fun expresionNormalizada(columna: String): String {
        var resultado = columna
        val equivalencias = listOf(
            'á' to 'a', 'Á' to 'a', 'é' to 'e', 'É' to 'e',
            'í' to 'i', 'Í' to 'i', 'ó' to 'o', 'Ó' to 'o',
            'ú' to 'u', 'Ú' to 'u', 'ü' to 'u', 'Ü' to 'u',
            'ñ' to 'n', 'Ñ' to 'n'
        )
        equivalencias.forEach { (acentuada, base) ->
            resultado = "REPLACE($resultado, '$acentuada', '$base')"
        }
        return "LOWER($resultado)"
    }

    private fun expresionDigitos(columna: String): String {
        var resultado = columna
        listOf(" ", "+", "-", "(", ")", ".", "/", "\\").forEach { caracter ->
            resultado = "REPLACE($resultado, '$caracter', '')"
        }
        return resultado
    }

    private fun mostrarResumen() {
        resumen.removeAllViews()
        GrupoTablero.entries.forEach { grupo ->
            val cantidad = grupo.estados.sumOf { conteos[it] ?: 0 }
            val button = MaterialButton(activity).apply {
                text = "${grupo.etiqueta}\n$cantidad"
                isAllCaps = false
                contentDescription = "${grupo.etiqueta}: $cantidad órdenes. Toca para filtrar."
                setOnClickListener {
                    grupoSeleccionado =
                        if (grupoSeleccionado == grupo) null else grupo
                    estadoSeleccionado = null
                    seleccionarEstado(null)
                    actualizarEtiquetaFiltro()
                    actualizarTablero(reiniciar = true)
                }
                if (grupoSeleccionado == grupo) {
                    setBackgroundColor(Color.rgb(0, 117, 112))
                    setTextColor(Color.WHITE)
                }
            }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(8) }
            resumen.addView(button, params)
        }
    }

    private fun configurarEstados(estados: List<String>) {
        val filtros = listOf(FiltroEstado(null, "Todos los estados")) +
            estados.map { FiltroEstado(it, ReglasTablero.etiquetaEstado(it)) }
        cambiandoFiltro = true
        selectorEstado.adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_dropdown_item,
            filtros
        )
        val selectedPosition = filtros.indexOfFirst { it.codigo == estadoSeleccionado }
            .coerceAtLeast(0)
        selectorEstado.setSelection(selectedPosition, false)
        cambiandoFiltro = false
    }

    private fun seleccionarEstado(estado: String?) {
        estadoSeleccionado = estado
        cambiandoFiltro = true
        val filtros = selectorEstado.adapter as? ArrayAdapter<*>
        val position = (0 until (filtros?.count ?: 0)).firstOrNull { index ->
            (selectorEstado.getItemAtPosition(index) as? FiltroEstado)?.codigo == estado
        } ?: 0
        selectorEstado.setSelection(position, false)
        cambiandoFiltro = false
    }

    private fun actualizarEtiquetaFiltro() {
        val partes = mutableListOf<String>()
        grupoSeleccionado?.let { partes += it.etiqueta }
        estadoSeleccionado?.let { partes += ReglasTablero.etiquetaEstado(it) }
        busqueda.text?.toString()?.takeIf(String::isNotBlank)?.let { partes += "búsqueda activa" }
        textoFiltro.text = if (partes.isEmpty()) {
            "Sin filtros aplicados"
        } else {
            "Filtro aplicado: ${partes.joinToString(" + ")}"
        }
        botonLimpiarFiltro.visibility = if (partes.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun limpiarFiltros() {
        handler.removeCallbacks(busquedaPendiente)
        grupoSeleccionado = null
        seleccionarEstado(null)
        busqueda.setText("")
        actualizarEtiquetaFiltro()
        actualizarTablero(reiniciar = true)
    }

    private fun mostrarEstadoSinResultados(estaVacia: Boolean, consulta: String) {
        listaOrdenes.removeAllViews()
        if (!estaVacia) {
            textoEstado.visibility = View.GONE
            return
        }
        val hayOrdenesActivas = conteos.values.sum() > 0
        textoEstado.text = when {
            consulta.isNotBlank() || grupoSeleccionado != null || estadoSeleccionado != null ->
                "No hay coincidencias con la búsqueda o el filtro aplicado."
            hayOrdenesActivas -> "No hay órdenes que coincidan con los filtros."
            else -> "Todavía no hay órdenes registradas."
        }
        textoEstado.visibility = View.VISIBLE
        if (!hayOrdenesActivas && consulta.isBlank() &&
            grupoSeleccionado == null && estadoSeleccionado == null
        ) {
            listaOrdenes.addView(
                texto(
                    "La recepción estará disponible en una etapa posterior. Los datos aún no se pueden registrar desde el tablero.",
                    14f,
                    COLOR_SECUNDARIO,
                    false
                )
            )
        }
    }

    private fun agregarTarjeta(tarjeta: TarjetaOrdenTablero) {
        val orden = tarjeta.orden
        val cliente = orden.cliente.takeIf(String::isNotBlank) ?: "Cliente no registrado"
        val equipo = listOf(orden.marca, orden.modelo)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .ifBlank { orden.tipoEquipo.takeIf(String::isNotBlank) ?: "Equipo no especificado" }
        val zona = zonaHoraria
        val recibida = runCatching {
            fechaFormato.withZone(zona).format(Instant.parse(orden.recibidaEn))
        }.getOrDefault("Fecha no disponible")
        val tarjetaView = MaterialCardView(activity).apply {
            radius = dp(14).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            contentDescription = buildString {
                append("Folio ${orden.folio}. $cliente. $equipo. ${tarjeta.estado}. ")
                append("Antigüedad ${tarjeta.antiguedad}. Tiempo en estado ${tarjeta.tiempoEnEstado}. ")
                append("Próxima acción: ${tarjeta.proximaAccion}.")
                tarjeta.alerta?.let { append(" ${it.titulo}. ${it.detalle}") }
            }
        }
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        body.addView(texto("Folio ${orden.folio}", 17f, COLOR_TEXTO, true))
        body.addView(texto(cliente, 16f, COLOR_TEXTO, true))
        body.addView(texto(equipo, 14f, COLOR_SECUNDARIO, false))
        body.addView(texto("Recibida: $recibida · Antigüedad: ${tarjeta.antiguedad}", 14f, COLOR_SECUNDARIO, false))
        body.addView(texto("Estado: ${tarjeta.estado} · En estado: ${tarjeta.tiempoEnEstado}", 14f, COLOR_TEXTO, true))
        body.addView(texto("Próxima acción: ${tarjeta.proximaAccion}", 14f, COLOR_SECUNDARIO, false))
        tarjeta.alerta?.let { alerta ->
            body.addView(texto("⚠ ${alerta.titulo}", 14f, COLOR_ALERTA, true))
            body.addView(texto(alerta.detalle, 13f, COLOR_ALERTA, false))
        }
        tarjetaView.addView(body)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        }
        listaOrdenes.addView(tarjetaView, params)
    }

    private fun cargarSiguientePagina() {
        if (!hayMas || cargando) return
        textoEstado.text = "Cargando más órdenes…"
        textoEstado.visibility = View.VISIBLE
        actualizarTablero(reiniciar = false)
    }

    private fun mostrarMenu() {
        val areas = listOf(
            "Tablero — Disponible",
            "Órdenes y recepción — Pendiente",
            "Clientes — Pendiente",
            "Dispositivos — Pendiente",
            "Servicios — Pendiente",
            "Inventario de piezas — Pendiente",
            "Caja — Pendiente",
            "Garantías — Pendiente",
            "Informes — Pendiente",
            "Configuración — Pendiente",
            "Respaldo — Pendiente"
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle("Áreas de trabajo")
            .setItems(areas.toTypedArray()) { _, position ->
                val mensaje = if (position == 0) {
                    "El tablero ya está abierto."
                } else {
                    "Esta área estará disponible en una etapa posterior."
                }
                Toast.makeText(activity, mensaje, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cerrar", null)
            .show()
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

    private fun margenSuperior(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(16) }

    private fun dp(valor: Int): Int =
        (valor * activity.resources.displayMetrics.density).toInt()

    companion object {
        private const val PAGE_SIZE = 50
        private const val DEBOUNCE_BUSQUEDA_MILLIS = 300L
        private val COLOR_TEXTO = Color.rgb(20, 32, 48)
        private val COLOR_SECUNDARIO = Color.rgb(91, 105, 122)
        private val COLOR_ALERTA = Color.rgb(151, 55, 20)
        private val fechaFormato =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX"))
    }
}
