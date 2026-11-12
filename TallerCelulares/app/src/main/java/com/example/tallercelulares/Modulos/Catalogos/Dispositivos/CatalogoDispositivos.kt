package com.example.tallercelulares.Modulos.Catalogos.Dispositivos

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.room.withTransaction
import androidx.lifecycle.lifecycleScope
import com.example.tallercelulares.DataBase.Entidades.Cliente.Cliente
import com.example.tallercelulares.DataBase.Entidades.Dispositivo.Dispositivo
import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.Modulos.Catalogos.Clientes.ReglasClientes
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant

internal class CatalogoDispositivos(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val clienteInicialId: String?,
    private val dispositivoInicialId: String? = null,
    private val alVolverAlTablero: () -> Unit,
    private val alVolverAlCliente: (String) -> Unit,
    private val alBloquear: () -> Unit
) {
    private enum class Pantalla { SELECTOR, LISTA, FICHA, FORMULARIO, CERRADA }
    private val handler = Handler(Looper.getMainLooper())
    private var recargarBusqueda: (() -> Unit)? = null
    private val busquedaPendiente = Runnable { recargarBusqueda?.invoke() }
    private val callbackAtras = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when (pantalla) {
                Pantalla.SELECTOR -> volverAlTablero()
                Pantalla.LISTA -> volverDelCatalogo()
                Pantalla.FICHA -> {
                    val volverAlFormulario = alVolverDeCoincidencia
                    alVolverDeCoincidencia = null
                    if (volverAlFormulario == null) mostrarLista(reiniciar = true) else volverAlFormulario()
                }
                Pantalla.FORMULARIO -> cancelarFormulario()
                Pantalla.CERRADA -> Unit
            }
        }
    }

    private var pantalla = Pantalla.SELECTOR
    private var clienteSeleccionado: String? = clienteInicialId
    private var filtro = ""
    private var desplazamiento = 0
    private var hayMas = false
    private var cargando = false
    private var tarea: Job? = null
    private var editorDe: String? = null
    private var dispositivoSeleccionado: String? = null
    private var alVolverDeCoincidencia: (() -> Unit)? = null
    private var filtroTexto: TextInputEditText? = null
    private var lista: LinearLayout? = null
    private var estado: TextView? = null
    private var botonMas: MaterialButton? = null
    private var botonReintentar: MaterialButton? = null

    init {
        activity.onBackPressedDispatcher.addCallback(activity, callbackAtras)
        when {
            dispositivoInicialId != null -> mostrarFicha(dispositivoInicialId)
            clienteInicialId != null -> mostrarLista(reiniciar = true)
            else -> mostrarSelectorClientes()
        }
    }

    fun refrescar() {
        when (pantalla) {
            Pantalla.SELECTOR -> actualizarSelector(true)
            Pantalla.LISTA -> actualizarLista(true)
            Pantalla.FICHA -> dispositivoSeleccionado?.let { mostrarFicha(it, alVolverDeCoincidencia) }
            Pantalla.FORMULARIO, Pantalla.CERRADA -> Unit
        }
    }

    fun cerrar() {
        handler.removeCallbacksAndMessages(null)
        tarea?.cancel()
        pantalla = Pantalla.CERRADA
        callbackAtras.isEnabled = false
    }

    private fun mostrarSelectorClientes() {
        pantalla = Pantalla.SELECTOR
        val contenido = crearContenido("Dispositivos", true)
        contenido.addView(texto("Selecciona un cliente activo para consultar sus equipos.", 15f, COLOR_SECUNDARIO))
        val campo = crearCampoBusqueda("Buscar cliente por nombre o teléfono")
        contenido.addView(campo.first, margenInferior())
        filtroTexto = campo.second.apply { setText(filtro) }
        estado = texto("Cargando clientes…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar clientes") {
            actualizarSelector(true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más clientes", "Cargar más clientes") {
            if (hayMas && !cargando) actualizarSelector(false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        observarBusqueda { actualizarSelector(true) }
        actualizarSelector(true)
    }

    private fun actualizarSelector(reiniciar: Boolean) {
        if (pantalla != Pantalla.SELECTOR) return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            vistaLista.removeAllViews()
            estadoVista.text = "Cargando clientes…"
            botonReintentar?.visibility = View.GONE
            botonMas?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = filtro.trim()
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val pagina = database.clienteDao().buscarActivos(
                    consulta = ReglasClientes.patronBusqueda(consulta),
                    telefono = ReglasClientes.digitosTelefono(consulta),
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val registros = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += registros.size
                registros.forEach { cliente ->
                    vistaLista.addView(
                        boton(
                            cliente.nombre,
                            "Seleccionar cliente ${cliente.nombre}"
                        ) { seleccionarCliente(cliente.id) },
                        margenInferior()
                    )
                }
                estadoVista.text = when {
                    registros.isNotEmpty() -> ""
                    consulta.isBlank() -> "No hay clientes activos. Registra un cliente antes de agregar dispositivos."
                    else -> "No se encontraron clientes. Prueba otra búsqueda."
                }
                estadoVista.visibility = if (registros.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = "No se pudieron consultar los clientes."
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun seleccionarCliente(id: String) {
        clienteSeleccionado = id
        filtro = ""
        mostrarLista(reiniciar = true)
    }

    private fun mostrarLista(reiniciar: Boolean) {
        val clienteId = clienteSeleccionado ?: run {
            mostrarSelectorClientes()
            return
        }
        pantalla = Pantalla.LISTA
        editorDe = null
        val contenido = crearContenido("Dispositivos del cliente", true)
        contenido.addView(texto("Cargando cliente…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val cliente = database.clienteDao().obtenerPorId(clienteId)
                    ?: error("No se encontró el cliente.")
                if (pantalla != Pantalla.LISTA || clienteSeleccionado != clienteId) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Dispositivos de ${cliente.nombre}")
                if (cliente.eliminadoEn != null) {
                    contenido.addView(
                        texto("Cliente inactivo. Los dispositivos se conservan para consulta.", 14f, COLOR_ALERTA),
                        margenInferior()
                    )
                } else {
                    contenido.addView(
                        boton("Agregar dispositivo", "Registrar un dispositivo para ${cliente.nombre}") {
                            mostrarFormulario()
                        },
                        margenInferior()
                    )
                }
                val buscar = crearCampoBusqueda("Buscar por tipo, marca, modelo o color")
                contenido.addView(buscar.first, margenInferior())
                filtroTexto = buscar.second.apply { setText(filtro) }
                estado = texto("Cargando dispositivos…", 15f, COLOR_SECUNDARIO)
                contenido.addView(estado)
                botonReintentar = boton("Reintentar", "Volver a consultar dispositivos") {
                    actualizarLista(true)
                }.apply { visibility = View.GONE }
                contenido.addView(botonReintentar)
                lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                contenido.addView(lista)
                botonMas = boton("Cargar más dispositivos", "Cargar más dispositivos") {
                    if (hayMas && !cargando) actualizarLista(false)
                }.apply { visibility = View.GONE }
                contenido.addView(botonMas)
                observarBusqueda { actualizarLista(true) }
                if (reiniciar) desplazamiento = 0
                actualizarLista(reiniciar)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Dispositivos del cliente")
                contenido.addView(texto("No se pudo consultar el cliente.", 15f, COLOR_ERROR), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a consultar el cliente") {
                    mostrarLista(reiniciar = true)
                })
            }
        }
    }

    private fun actualizarLista(reiniciar: Boolean) {
        if (pantalla != Pantalla.LISTA) return
        val clienteId = clienteSeleccionado ?: return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            vistaLista.removeAllViews()
            estadoVista.text = "Cargando dispositivos…"
            estadoVista.visibility = View.VISIBLE
            botonMas?.visibility = View.GONE
            botonReintentar?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasDispositivos.patronBusqueda(filtro)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val pagina = database.dispositivoDao().buscarActivosPorCliente(
                    clienteId = clienteId,
                    consulta = consulta,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val registros = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += registros.size
                registros.forEach(::agregarDispositivo)
                estadoVista.text = when {
                    registros.isNotEmpty() -> ""
                    filtro.isBlank() -> "Este cliente no tiene dispositivos activos."
                    else -> "No hay dispositivos que coincidan. Prueba otra búsqueda."
                }
                estadoVista.visibility = if (registros.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = "No se pudieron consultar los dispositivos."
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun agregarDispositivo(dispositivo: Dispositivo) {
        val identificacion = listOf(dispositivo.tipo, dispositivo.marca, dispositivo.modelo, dispositivo.color)
            .filter(String::isNotBlank)
            .joinToString(" · ")
        lista?.addView(
            boton(identificacion, "Abrir ficha de $identificacion") {
                mostrarFicha(dispositivo.id)
            },
            margenInferior()
        )
    }

    private fun mostrarFicha(id: String, alVolver: (() -> Unit)? = null) {
        pantalla = Pantalla.FICHA
        dispositivoSeleccionado = id
        alVolverDeCoincidencia = alVolver
        val contenido = crearContenido("Ficha del dispositivo", true)
        contenido.addView(texto("Cargando dispositivo…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val dispositivo = database.dispositivoDao().obtenerPorId(id)
                    ?: error("No se encontró el dispositivo.")
                val clienteId = clienteSeleccionado
                if (clienteId != null && dispositivo.clienteId != clienteId) {
                    error("El dispositivo no pertenece al cliente seleccionado.")
                }
                val cliente = database.clienteDao().obtenerPorId(dispositivo.clienteId)
                    ?: error("No se encontró el cliente relacionado.")
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del dispositivo")
                contenido.addView(texto("Cliente: ${cliente.nombre}", 16f, COLOR_TEXTO), margenInferior())
                if (dispositivo.eliminadoEn != null) {
                    contenido.addView(
                        texto("Dispositivo inactivo. Su historial se conserva.", 15f, COLOR_ALERTA),
                        margenInferior()
                    )
                }
                contenido.addView(texto(dispositivo.tipo, 18f, COLOR_TEXTO, true), margenSuperior())
                contenido.addView(texto("Marca: ${dispositivo.marca}", 16f, COLOR_TEXTO))
                contenido.addView(texto("Modelo: ${dispositivo.modelo}", 16f, COLOR_TEXTO))
                contenido.addView(texto("Color: ${dispositivo.color.ifBlank { "No registrado" }}", 16f, COLOR_TEXTO))
                agregarIdentificador(contenido, "Número de serie", dispositivo.numeroSerie)
                agregarIdentificador(contenido, "IMEI", dispositivo.imei)
                if (dispositivo.eliminadoEn == null) {
                    contenido.addView(
                        boton("Editar dispositivo", "Editar datos actuales del dispositivo") {
                            mostrarFormulario(dispositivo.id)
                        },
                        margenSuperior()
                    )
                    contenido.addView(
                        boton("Dar de baja", "Dar de baja el dispositivo sin borrar su historial") {
                            confirmarBaja(dispositivo)
                        },
                        margenInferior()
                    )
                }
                contenido.addView(texto("Historial de órdenes", 19f, COLOR_TEXTO, true), margenSuperior())
                val estadoHistorial = texto("Cargando órdenes…", 15f, COLOR_SECUNDARIO)
                contenido.addView(estadoHistorial)
                val seccionHistorial = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                contenido.addView(seccionHistorial)
                lateinit var reintentarHistorial: MaterialButton
                reintentarHistorial =
                    boton("Reintentar historial", "Volver a consultar las órdenes del dispositivo") {
                        cargarHistorial(dispositivo.id, estadoHistorial, seccionHistorial, reintentarHistorial)
                    }
                reintentarHistorial.visibility = View.GONE
                contenido.addView(reintentarHistorial)
                cargarHistorial(dispositivo.id, estadoHistorial, seccionHistorial, reintentarHistorial)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del dispositivo")
                contenido.addView(texto("No se pudo consultar el dispositivo.", 15f, COLOR_ERROR), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a consultar el dispositivo") {
                    mostrarFicha(id, alVolver)
                })
            }
        }
    }

    private fun cargarHistorial(
        dispositivoId: String,
        estadoHistorial: TextView,
        seccion: LinearLayout,
        it: MaterialButton?
    ) {
        it?.visibility = View.GONE
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ordenes = database.ordenServicioDao().obtenerPorDispositivoId(dispositivoId)
                if (pantalla != Pantalla.FICHA) return@launch
                seccion.removeAllViews()
                estadoHistorial.visibility = if (ordenes.isEmpty()) View.VISIBLE else View.GONE
                estadoHistorial.text = if (ordenes.isEmpty()) "Sin órdenes relacionadas." else ""
                ordenes.forEach { orden ->
                    seccion.addView(
                        texto(resumenOrden(orden), 15f, COLOR_TEXTO),
                        margenInferior()
                    )
                }
                it?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoHistorial.text = "No se pudo consultar el historial."
                estadoHistorial.visibility = View.VISIBLE
                it?.visibility = View.VISIBLE
            }
        }
    }

    private fun resumenOrden(orden: OrdenServicio): String = buildString {
        append("${orden.folio} · ${orden.estado}")
        append("\nRecibida: ${orden.recibidaEn}")
        if (orden.eliminadaEn != null) append("\nOrden archivada")
    }

    private fun agregarIdentificador(contenido: LinearLayout, etiqueta: String, valor: String?) {
        contenido.addView(texto(etiqueta, 15f, COLOR_SECUNDARIO, true), margenSuperior())
        val campo = texto(ReglasDispositivos.identificadorEnmascarado(valor), 16f, COLOR_TEXTO)
        contenido.addView(campo)
        var revelado = false
        val revelar = boton("Mostrar", "Revelar $etiqueta explícitamente") {}
        revelar.setOnClickListener {
            if (valor.isNullOrBlank()) {
                campo.text = "No registrado"
            } else if (revelado) {
                campo.text = ReglasDispositivos.identificadorEnmascarado(valor)
                revelar.text = "Mostrar"
                revelado = false
            } else {
                campo.text = valor
                revelar.text = "Ocultar"
                revelado = true
            }
        }
        contenido.addView(revelar, margenInferior())
    }

    private fun mostrarFormulario(
        dispositivoId: String? = null,
        borrador: DatosDispositivoNormalizados? = null
    ) {
        pantalla = Pantalla.FORMULARIO
        editorDe = dispositivoId
        tarea?.cancel()
        val contenido = crearContenido(
            if (dispositivoId == null) "Nuevo dispositivo" else "Editar dispositivo",
            true
        )
        contenido.addView(texto("Preparando formulario…", 15f, COLOR_SECUNDARIO))
        tarea = activity.lifecycleScope.launch {
            try {
                val clienteId = clienteSeleccionado ?: error("No se seleccionó un cliente.")
                val cliente = database.clienteDao().obtenerActivoPorId(clienteId)
                    ?: error("El cliente ya no está activo.")
                val dispositivo = dispositivoId?.let {
                    database.dispositivoDao().obtenerActivoPorId(it)
                        ?: error("El dispositivo ya no está activo.")
                }
                if (dispositivo != null && dispositivo.clienteId != clienteId) {
                    error("El dispositivo no pertenece al cliente seleccionado.")
                }
                if (pantalla != Pantalla.FORMULARIO) return@launch
                construirFormulario(contenido, cliente, dispositivo, borrador)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                contenido.removeAllViews()
                agregarEncabezado(
                    contenido,
                    if (dispositivoId == null) "Nuevo dispositivo" else "Editar dispositivo"
                )
                contenido.addView(
                    texto("No se pudo preparar el formulario. Verifica que el cliente y el dispositivo sigan activos.", 15f, COLOR_ERROR),
                    margenInferior()
                )
                contenido.addView(boton("Reintentar", "Volver a cargar el formulario") {
                    mostrarFormulario(dispositivoId, borrador)
                })
            }
        }
    }

    private fun construirFormulario(
        contenido: LinearLayout,
        cliente: Cliente,
        dispositivo: Dispositivo?,
        borrador: DatosDispositivoNormalizados?
    ) {
        contenido.removeAllViews()
        agregarEncabezado(contenido, if (dispositivo == null) "Nuevo dispositivo" else "Editar dispositivo")
        contenido.addView(texto("Cliente: ${cliente.nombre}", 15f, COLOR_SECUNDARIO), margenInferior())
        contenido.addView(
            texto("Tipo, marca y modelo son obligatorios. Color, serie e IMEI son opcionales.", 14f, COLOR_SECUNDARIO),
            margenInferior()
        )
        val layouts = mutableMapOf<String, TextInputLayout>()
        val campos = mutableMapOf<String, TextInputEditText>()
        agregarCampo(contenido, layouts, campos, "tipo", "Tipo", borrador?.tipo ?: dispositivo?.tipo.orEmpty())
        agregarCampo(contenido, layouts, campos, "marca", "Marca", borrador?.marca ?: dispositivo?.marca.orEmpty())
        agregarCampo(contenido, layouts, campos, "modelo", "Modelo", borrador?.modelo ?: dispositivo?.modelo.orEmpty())
        agregarCampo(contenido, layouts, campos, "color", "Color (opcional)", borrador?.color ?: dispositivo?.color.orEmpty())
        agregarCampo(contenido, layouts, campos, "numeroSerie", "Número de serie (opcional)", borrador?.numeroSerie ?: dispositivo?.numeroSerie.orEmpty())
        agregarCampo(contenido, layouts, campos, "imei", "IMEI (opcional)", borrador?.imei ?: dispositivo?.imei.orEmpty())
        val mensaje = texto("", 14f, COLOR_ERROR)
        contenido.addView(mensaje, margenInferior())
        val guardar = boton("Guardar dispositivo", "Guardar datos actuales del dispositivo") {}
        guardar.setOnClickListener {
            layouts.values.forEach { it.error = null }
            val valores = listOf("tipo", "marca", "modelo", "color", "numeroSerie", "imei")
                .associateWith { campos[it]?.text?.toString().orEmpty() }
            val errores = ReglasDispositivos.validar(
                tipo = valores.getValue("tipo"),
                marca = valores.getValue("marca"),
                modelo = valores.getValue("modelo"),
                color = valores.getValue("color"),
                numeroSerie = valores.getValue("numeroSerie"),
                imei = valores.getValue("imei")
            )
            errores.forEach { (campo, error) -> layouts[campo]?.error = error }
            if (errores.isNotEmpty()) return@setOnClickListener
            guardar.isEnabled = false
            mensaje.text = "Guardando…"
            val datos = ReglasDispositivos.normalizar(
                tipo = valores.getValue("tipo"),
                marca = valores.getValue("marca"),
                modelo = valores.getValue("modelo"),
                color = valores.getValue("color"),
                numeroSerie = valores.getValue("numeroSerie"),
                imei = valores.getValue("imei")
            )
            if (dispositivo == null) revisarCoincidencias(
                cliente.id, datos, guardar, mensaje
            ) else guardarEdicion(dispositivo, datos, guardar, mensaje)
        }
        contenido.addView(guardar, margenInferior())
    }

    private fun revisarCoincidencias(
        clienteId: String,
        datos: DatosDispositivoNormalizados,
        guardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            try {
                val coincidencias = if (datos.imei == null && datos.numeroSerie == null) {
                    emptyList()
                } else {
                    database.dispositivoDao().buscarCoincidenciasActivas(
                        clienteId = clienteId,
                        imei = datos.imei,
                        numeroSerie = datos.numeroSerie,
                        limite = LIMITE_COINCIDENCIAS
                    )
                }
                if (pantalla != Pantalla.FORMULARIO) return@launch
                if (coincidencias.isEmpty()) {
                    crearDispositivo(clienteId, datos, guardar, mensaje)
                    return@launch
                }
                val opciones = coincidencias.map {
                    listOf(it.tipo, it.marca, it.modelo, it.color)
                        .filter(String::isNotBlank)
                        .joinToString(" · ")
                }.toTypedArray()
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Posibles dispositivos coincidentes")
                    .setMessage("La coincidencia no confirma que sea el mismo equipo. Puedes revisar una opción o continuar con el alta.")
                    .setItems(opciones) { _, posicion ->
                        mostrarFicha(coincidencias[posicion].id) {
                            mostrarFormulario(borrador = datos)
                        }
                    }
                    .setPositiveButton("Crear de todos modos") { _, _ ->
                        crearDispositivo(clienteId, datos, guardar, mensaje)
                    }
                    .setNegativeButton("Seguir revisando") { _, _ ->
                        guardar.isEnabled = true
                        mensaje.text = ""
                    }
                    .setOnCancelListener {
                        guardar.isEnabled = true
                        mensaje.text = ""
                    }
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                guardar.isEnabled = true
                mensaje.text = "No se pudieron revisar posibles coincidencias. Intenta guardar de nuevo."
            }
        }
    }

    private fun crearDispositivo(
        clienteId: String,
        datos: DatosDispositivoNormalizados,
        guardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                val nuevo = Dispositivo(
                    id = EntidadId.nuevo(),
                    clienteId = clienteId,
                    tipo = datos.tipo,
                    marca = datos.marca,
                    modelo = datos.modelo,
                    color = datos.color,
                    numeroSerie = datos.numeroSerie,
                    imei = datos.imei,
                    creadoEn = ahora,
                    actualizadoEn = ahora
                )
                val guardado = database.withTransaction {
                    if (database.clienteDao().obtenerActivoPorId(clienteId) == null) {
                        error("El cliente ya no está activo.")
                    }
                    database.dispositivoDao().insertar(nuevo)
                    database.dispositivoDao().obtenerActivoPorId(nuevo.id)
                        ?: error("No se pudo volver a consultar el dispositivo guardado.")
                }
                if (guardado.clienteId != clienteId) error("El dispositivo no pertenece al cliente.")
                if (
                    guardado.tipo != datos.tipo ||
                    guardado.marca != datos.marca ||
                    guardado.modelo != datos.modelo ||
                    guardado.color != datos.color ||
                    guardado.numeroSerie != datos.numeroSerie ||
                    guardado.imei != datos.imei
                ) {
                    error("No se pudieron confirmar los datos guardados.")
                }
                filtro = ""
                mostrarLista(reiniciar = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                guardar.isEnabled = true
                mensaje.text = "No se pudo guardar el dispositivo. Tus datos siguen en el formulario."
            }
        }
    }

    private fun guardarEdicion(
        dispositivo: Dispositivo,
        datos: DatosDispositivoNormalizados,
        guardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            try {
                val actualizados = database.dispositivoDao().actualizarDatos(
                    id = dispositivo.id,
                    clienteId = dispositivo.clienteId,
                    tipo = datos.tipo,
                    marca = datos.marca,
                    modelo = datos.modelo,
                    color = datos.color,
                    numeroSerie = datos.numeroSerie,
                    imei = datos.imei,
                    actualizadoEn = Instant.now().toString()
                )
                if (actualizados != 1) error("El dispositivo ya no está activo.")
                val actualizado = database.dispositivoDao().obtenerActivoPorId(dispositivo.id)
                    ?: error("No se pudo volver a consultar el dispositivo actualizado.")
                if (actualizado.clienteId != dispositivo.clienteId) {
                    error("No se pudo confirmar el propietario del dispositivo.")
                }
                if (
                    actualizado.tipo != datos.tipo ||
                    actualizado.marca != datos.marca ||
                    actualizado.modelo != datos.modelo ||
                    actualizado.color != datos.color ||
                    actualizado.numeroSerie != datos.numeroSerie ||
                    actualizado.imei != datos.imei
                ) {
                    error("No se pudieron confirmar los datos actualizados.")
                }
                mostrarFicha(dispositivo.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                guardar.isEnabled = true
                mensaje.text = "No se pudo actualizar el dispositivo. Tus cambios siguen en el formulario."
            }
        }
    }

    private fun confirmarBaja(dispositivo: Dispositivo) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ordenes = database.ordenServicioDao().obtenerPorDispositivoId(dispositivo.id)
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Dar de baja el dispositivo?")
                    .setMessage(
                        "Dejará de aparecer entre los dispositivos activos. " +
                            "Se conservarán el equipo y sus ${ordenes.size} órdenes relacionadas."
                    )
                    .setPositiveButton("Confirmar baja") { _, _ -> realizarBaja(dispositivo.id) }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo consultar el historial")
                    .setMessage("No se confirmó la baja. Intenta de nuevo.")
                    .setPositiveButton("Reintentar") { _, _ -> confirmarBaja(dispositivo) }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun realizarBaja(id: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                val actualizados = database.dispositivoDao().darDeBaja(id, ahora, ahora)
                if (actualizados != 1) error("El dispositivo ya no está activo.")
                mostrarFicha(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo dar de baja")
                    .setMessage("El dispositivo se conserva sin cambios. Intenta de nuevo.")
                    .setPositiveButton("Reintentar") { _, _ -> realizarBaja(id) }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
        }
    }

    private fun cancelarFormulario() {
        val id = editorDe
        if (id != null) mostrarFicha(id) else mostrarLista(reiniciar = true)
    }

    private fun volverDelCatalogo() {
        val clienteId = clienteInicialId
        if (clienteId == null) volverAlTablero() else {
            cerrar()
            alVolverAlCliente(clienteId)
        }
    }

    private fun volverAlTablero() {
        cerrar()
        alVolverAlTablero()
    }

    private fun crearContenido(titulo: String, mostrarAtras: Boolean): LinearLayout {
        container.removeAllViews()
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            contentDescription = titulo
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
        container.addView(
            scroll,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        agregarEncabezado(contenido, titulo, mostrarAtras)
        return contenido
    }

    private fun agregarEncabezado(
        contenido: LinearLayout,
        titulo: String,
        mostrarAtras: Boolean = true
    ) {
        val encabezado = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        encabezado.addView(
            texto(titulo, 22f, COLOR_TEXTO, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (mostrarAtras) {
            encabezado.addView(boton("Atrás", "Volver a la pantalla anterior") {
                callbackAtras.handleOnBackPressed()
            })
        }
        encabezado.addView(boton("Bloquear", "Bloquear la aplicación") { cerrar(); alBloquear() })
        contenido.addView(encabezado, margenInferior())
    }

    private fun agregarCampo(
        contenido: LinearLayout,
        layouts: MutableMap<String, TextInputLayout>,
        campos: MutableMap<String, TextInputEditText>,
        clave: String,
        etiqueta: String,
        valor: String,
        tipo: Int = InputType.TYPE_CLASS_TEXT
    ) {
        val layout = TextInputLayout(activity).apply {
            hint = etiqueta
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val campo = TextInputEditText(layout.context).apply {
            inputType = tipo
            contentDescription = etiqueta
            setSingleLine(true)
            setText(valor)
        }
        layout.addView(campo)
        contenido.addView(layout, margenInferior())
        layouts[clave] = layout
        campos[clave] = campo
    }

    private fun crearCampoBusqueda(hint: String): Pair<TextInputLayout, TextInputEditText> {
        val layout = TextInputLayout(activity).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        val campo = TextInputEditText(layout.context).apply {
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            contentDescription = hint
        }
        layout.addView(campo)
        return layout to campo
    }

    private fun observarBusqueda(accion: () -> Unit) {
        recargarBusqueda = accion
        filtroTexto?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtro = s?.toString().orEmpty()
                handler.removeCallbacks(busquedaPendiente)
                handler.postDelayed(busquedaPendiente, BUSQUEDA_DEBOUNCE_MS)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun boton(titulo: String, descripcion: String, accion: () -> Unit): MaterialButton =
        MaterialButton(activity).apply {
            text = titulo
            isAllCaps = false
            contentDescription = descripcion
            setOnClickListener { accion() }
        }

    private fun texto(valor: String, tamano: Float, color: Int, negrita: Boolean = false): TextView =
        TextView(activity).apply {
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
        ).apply { topMargin = dp(12) }

    private fun dp(valor: Int): Int =
        (valor * activity.resources.displayMetrics.density).toInt()

    companion object {
        private const val PAGE_SIZE = 50
        private const val LIMITE_COINCIDENCIAS = 5
        private const val BUSQUEDA_DEBOUNCE_MS = 300L
        private val COLOR_TEXTO = Color.rgb(20, 32, 48)
        private val COLOR_SECUNDARIO = Color.rgb(91, 105, 122)
        private val COLOR_ERROR = Color.rgb(151, 55, 20)
        private val COLOR_ALERTA = Color.rgb(151, 55, 20)
    }
}
