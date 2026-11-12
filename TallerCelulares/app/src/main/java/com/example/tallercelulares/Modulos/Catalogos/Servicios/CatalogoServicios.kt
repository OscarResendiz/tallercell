package com.example.tallercelulares.Modulos.Catalogos.Servicios

import android.database.sqlite.SQLiteConstraintException
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
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicio
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.Cotizacion
import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacion
import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Currency

internal class CatalogoServicios(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val alVolverAlTablero: () -> Unit,
    private val alBloquear: () -> Unit
) {
    private enum class Pantalla {
        CATALOGO, FICHA, FORMULARIO, ORDENES, COTIZACION, SERVICIOS_COTIZACION, CERRADA
    }

    private class ErrorModulo(val mensajeUsuario: String) : Exception(mensajeUsuario)
    private class CodigoDuplicado : Exception()

    private val handler = Handler(Looper.getMainLooper())
    private var recargarBusqueda: (() -> Unit)? = null
    private val busquedaPendiente = Runnable { recargarBusqueda?.invoke() }
    private val callbackAtras = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            regresar()
        }
    }

    private var pantalla = Pantalla.CATALOGO
    private var tarea: Job? = null
    private var codigoEditado: String? = null
    private var filtroCatalogo = ""
    private var filtroOrdenes = ""
    private var filtroSelectorServicios = ""
    private var incluirInactivos = false
    private var cotizacionActualId: String? = null
    private var desplazamiento = 0
    private var hayMas = false
    private var cargando = false
    private var estado: TextView? = null
    private var lista: LinearLayout? = null
    private var botonMas: MaterialButton? = null
    private var botonReintentar: MaterialButton? = null

    init {
        activity.onBackPressedDispatcher.addCallback(activity, callbackAtras)
        mostrarCatalogo(true)
    }

    fun refrescar() {
        when (pantalla) {
            Pantalla.CATALOGO -> actualizarCatalogo(true)
            Pantalla.FICHA -> codigoEditado?.let(::mostrarFicha)
            Pantalla.ORDENES -> actualizarOrdenes(true)
            Pantalla.SERVICIOS_COTIZACION -> actualizarServiciosParaCotizacion(true)
            Pantalla.COTIZACION -> cotizacionActualId?.let(::mostrarCotizacion)
            Pantalla.FORMULARIO, Pantalla.CERRADA -> Unit
        }
    }

    fun cerrar() {
        handler.removeCallbacksAndMessages(null)
        tarea?.cancel()
        pantalla = Pantalla.CERRADA
        callbackAtras.isEnabled = false
    }

    private fun regresar() {
        when (pantalla) {
            Pantalla.CATALOGO -> {
                cerrar()
                alVolverAlTablero()
            }
            Pantalla.FICHA -> mostrarCatalogo(true)
            Pantalla.FORMULARIO -> {
                val id = codigoEditado
                if (id == null) mostrarCatalogo(true) else mostrarFicha(id)
            }
            Pantalla.ORDENES -> mostrarCatalogo(true)
            Pantalla.COTIZACION -> mostrarOrdenes(true)
            Pantalla.SERVICIOS_COTIZACION -> cotizacionActualId?.let(::mostrarCotizacion)
                ?: mostrarOrdenes(true)
            Pantalla.CERRADA -> Unit
        }
    }

    private fun mostrarCatalogo(reiniciar: Boolean) {
        pantalla = Pantalla.CATALOGO
        codigoEditado = null
        val contenido = crearContenido("Servicios")
        contenido.addView(
            boton("Nuevo servicio", "Registrar un servicio") { mostrarFormulario() },
            margenInferior()
        )
        contenido.addView(
            boton("Crear cotización para una orden", "Seleccionar una orden y agregar servicios a un borrador") {
                mostrarOrdenes(true)
            },
            margenInferior()
        )
        lateinit var botonFiltro: MaterialButton
        botonFiltro = boton(
            if (incluirInactivos) "Ocultar servicios inactivos" else "Mostrar servicios inactivos",
            "Cambiar la visibilidad de los servicios inactivos"
        ) {
            incluirInactivos = !incluirInactivos
            botonFiltro.text =
                if (incluirInactivos) "Ocultar servicios inactivos" else "Mostrar servicios inactivos"
            actualizarCatalogo(true)
        }
        contenido.addView(botonFiltro, margenInferior())
        val buscador = crearBuscador("Buscar por nombre o código", filtroCatalogo) { valor ->
            filtroCatalogo = valor
            programarBusqueda { actualizarCatalogo(true) }
        }
        contenido.addView(buscador, margenInferior())
        estado = texto("Cargando servicios…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar servicios") {
            actualizarCatalogo(true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más servicios", "Cargar más servicios") {
            if (hayMas && !cargando) actualizarCatalogo(false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        if (reiniciar) desplazamiento = 0
        actualizarCatalogo(reiniciar)
    }

    private fun actualizarCatalogo(reiniciar: Boolean) {
        if (pantalla != Pantalla.CATALOGO) return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            vistaLista.removeAllViews()
            estadoVista.text = "Cargando servicios…"
            estadoVista.visibility = View.VISIBLE
            botonMas?.visibility = View.GONE
            botonReintentar?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasServicios.patronBusqueda(filtroCatalogo)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorModulo("No se encontró la moneda configurada para el taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                val pagina = database.catalogoServicioDao().buscar(
                    consulta = consulta,
                    incluirInactivos = incluirInactivos,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val servicios = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += servicios.size
                servicios.forEach { servicio ->
                    vistaLista.addView(
                        boton(
                            textoFilaServicio(servicio, configuracion.monedaCodigo),
                            "Abrir servicio ${servicio.nombre}"
                        ) { mostrarFicha(servicio.id) },
                        margenInferior()
                    )
                }
                estadoVista.text = when {
                    servicios.isNotEmpty() -> ""
                    filtroCatalogo.isNotBlank() -> "No se encontraron servicios. Prueba otra búsqueda."
                    incluirInactivos -> "No hay servicios registrados."
                    else -> "No hay servicios activos. Puedes registrar el primero."
                }
                estadoVista.visibility = if (servicios.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = mensajeError(exception, "No se pudieron consultar los servicios.")
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun mostrarFicha(id: String) {
        pantalla = Pantalla.FICHA
        codigoEditado = id
        val contenido = crearContenido("Ficha del servicio")
        contenido.addView(texto("Cargando servicio…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val servicio = database.catalogoServicioDao().obtenerPorId(id)
                    ?: throw ErrorModulo("No se encontró el servicio.")
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorModulo("No se encontró la moneda configurada para el taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del servicio")
                contenido.addView(texto(servicio.nombre, 22f, COLOR_TEXTO, true))
                contenido.addView(texto("Código: ${servicio.codigo ?: "Sin código"}", 16f, COLOR_TEXTO))
                contenido.addView(texto("Descripción", 17f, COLOR_TEXTO, true), margenSuperior())
                contenido.addView(texto(servicio.descripcion, 16f, COLOR_TEXTO), margenInferior())
                contenido.addView(
                    texto("Precio base: ${ReglasServicios.formatearPrecio(servicio.precioBaseMinor, configuracion.monedaCodigo)}", 17f, COLOR_TEXTO, true),
                    margenInferior()
                )
                contenido.addView(
                    texto(if (servicio.activo) "Activo" else "Inactivo", 15f, if (servicio.activo) COLOR_SECUNDARIO else COLOR_ALERTA),
                    margenInferior()
                )
                if (servicio.activo) {
                    contenido.addView(boton("Editar", "Editar datos del servicio") {
                        mostrarFormulario(servicio.id)
                    }, margenInferior())
                    contenido.addView(boton("Desactivar", "Desactivar el servicio sin borrar cotizaciones") {
                        confirmarDesactivacion(servicio)
                    }, margenInferior())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del servicio")
                contenido.addView(texto(mensajeError(exception, "No se pudo consultar el servicio."), 15f, COLOR_ERROR), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a consultar el servicio") {
                    mostrarFicha(id)
                })
            }
        }
    }

    private fun mostrarFormulario(servicioId: String? = null) {
        pantalla = Pantalla.FORMULARIO
        codigoEditado = servicioId
        val contenido = crearContenido(if (servicioId == null) "Nuevo servicio" else "Editar servicio")
        contenido.addView(texto("Preparando formulario…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val servicio = servicioId?.let { id ->
                    database.catalogoServicioDao().obtenerPorId(id)
                        ?.takeIf(CatalogoServicio::activo)
                        ?: throw ErrorModulo("El servicio ya no está activo.")
                }
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorModulo("No se encontró la moneda configurada para el taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                if (pantalla != Pantalla.FORMULARIO) return@launch
                construirFormulario(contenido, servicio, configuracion.monedaCodigo)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FORMULARIO) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, if (servicioId == null) "Nuevo servicio" else "Editar servicio")
                contenido.addView(texto(mensajeError(exception, "No se pudo preparar el formulario."), 15f, COLOR_ERROR), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a cargar el formulario") {
                    mostrarFormulario(servicioId)
                })
            }
        }
    }

    private fun construirFormulario(
        contenido: LinearLayout,
        servicio: CatalogoServicio?,
        monedaCodigo: String
    ) {
        contenido.removeAllViews()
        agregarEncabezado(contenido, if (servicio == null) "Nuevo servicio" else "Editar servicio")
        contenido.addView(texto("La moneda del precio es $monedaCodigo.", 14f, COLOR_SECUNDARIO), margenInferior())
        val layouts = mutableMapOf<String, TextInputLayout>()
        val campos = mutableMapOf<String, TextInputEditText>()
        agregarCampo(contenido, layouts, campos, "codigo", "Código (opcional)", servicio?.codigo.orEmpty())
        agregarCampo(contenido, layouts, campos, "nombre", "Nombre", servicio?.nombre.orEmpty())
        agregarCampo(
            contenido,
            layouts,
            campos,
            "descripcion",
            "Descripción",
            servicio?.descripcion.orEmpty(),
            multilinea = true
        )
        agregarCampo(
            contenido,
            layouts,
            campos,
            "precio",
            "Precio base ($monedaCodigo)",
            servicio?.let { ReglasServicios.textoPrecioBase(it.precioBaseMinor, monedaCodigo) }.orEmpty(),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        )
        val mensaje = texto("", 14f, COLOR_ERROR)
        contenido.addView(mensaje, margenInferior())
        val guardar = boton("Guardar servicio", "Guardar servicio") {}
        guardar.setOnClickListener {
            layouts.values.forEach { it.error = null }
            val codigo = campos.getValue("codigo").text?.toString().orEmpty()
            val nombre = campos.getValue("nombre").text?.toString().orEmpty()
            val descripcion = campos.getValue("descripcion").text?.toString().orEmpty()
            val precio = campos.getValue("precio").text?.toString().orEmpty()
            val errores = ReglasServicios.validar(codigo, nombre, descripcion, precio, monedaCodigo)
            errores.forEach { (campo, error) -> layouts[campo]?.error = error }
            if (errores.isNotEmpty()) return@setOnClickListener
            val precioMinor = ReglasServicios.precioBaseMinor(precio, monedaCodigo)
            if (precioMinor == null) {
                layouts["precio"]?.error = "Escribe un precio válido para la moneda del taller."
                return@setOnClickListener
            }
            guardar.isEnabled = false
            mensaje.text = "Guardando…"
            guardarServicio(
                servicio = servicio,
                codigo = ReglasServicios.normalizarCodigo(codigo),
                nombre = nombre.trim(),
                descripcion = descripcion.trim(),
                precioBaseMinor = precioMinor,
                botonGuardar = guardar,
                mensaje = mensaje,
                errorCodigo = layouts["codigo"]
            )
        }
        contenido.addView(guardar, margenInferior())
    }

    private fun guardarServicio(
        servicio: CatalogoServicio?,
        codigo: String?,
        nombre: String,
        descripcion: String,
        precioBaseMinor: Long,
        botonGuardar: MaterialButton,
        mensaje: TextView,
        errorCodigo: TextInputLayout?
    ) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                database.withTransaction {
                    if (codigo != null && database.catalogoServicioDao().existeCodigo(codigo, servicio?.id)) {
                        throw CodigoDuplicado()
                    }
                    val ahora = Instant.now().toString()
                    if (servicio == null) {
                        val nuevo = CatalogoServicio(
                            id = EntidadId.nuevo(),
                            codigo = codigo,
                            nombre = nombre,
                            descripcion = descripcion,
                            precioBaseMinor = precioBaseMinor,
                            activo = true,
                            creadoEn = ahora,
                            actualizadoEn = ahora
                        )
                        database.catalogoServicioDao().insertar(nuevo)
                        val guardado = database.catalogoServicioDao().obtenerPorId(nuevo.id)
                            ?: error("No se pudo volver a consultar el servicio guardado.")
                        verificarServicio(guardado, codigo, nombre, descripcion, precioBaseMinor)
                    } else {
                        val actualizados = database.catalogoServicioDao().actualizarDatos(
                            id = servicio.id,
                            codigo = codigo,
                            nombre = nombre,
                            descripcion = descripcion,
                            precioBaseMinor = precioBaseMinor,
                            actualizadoEn = ahora
                        )
                        if (actualizados != 1) throw ErrorModulo("El servicio ya no está activo.")
                        val actualizado = database.catalogoServicioDao().obtenerPorId(servicio.id)
                            ?: error("No se pudo volver a consultar el servicio actualizado.")
                        verificarServicio(actualizado, codigo, nombre, descripcion, precioBaseMinor)
                    }
                }
                mostrarCatalogo(true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: CodigoDuplicado) {
                botonGuardar.isEnabled = true
                errorCodigo?.error = "Ya existe un servicio con este código."
                mensaje.text = "Corrige el código o déjalo vacío para continuar."
            } catch (exception: SQLiteConstraintException) {
                manejarConflictoOError(
                    exception,
                    codigo,
                    servicio?.id,
                    botonGuardar,
                    mensaje,
                    errorCodigo
                )
            } catch (exception: Exception) {
                botonGuardar.isEnabled = true
                mensaje.text = mensajeError(exception, "No se pudo guardar el servicio. Tus datos siguen en el formulario.")
            }
        }
    }

    private fun verificarServicio(
        servicio: CatalogoServicio,
        codigo: String?,
        nombre: String,
        descripcion: String,
        precioBaseMinor: Long
    ) {
        if (
            !servicio.activo ||
            servicio.codigo != codigo ||
            servicio.nombre != nombre ||
            servicio.descripcion != descripcion ||
            servicio.precioBaseMinor != precioBaseMinor
        ) {
            error("No se pudieron confirmar los datos guardados.")
        }
    }

    private suspend fun manejarConflictoOError(
        exception: SQLiteConstraintException,
        codigo: String?,
        excluirId: String?,
        botonGuardar: MaterialButton,
        mensaje: TextView,
        errorCodigo: TextInputLayout?
    ) {
        try {
            val duplicado = codigo != null &&
                database.catalogoServicioDao().existeCodigo(codigo, excluirId)
            botonGuardar.isEnabled = true
            if (duplicado) {
                errorCodigo?.error = "Ya existe un servicio con este código."
                mensaje.text = "Corrige el código o déjalo vacío para continuar."
            } else {
                mensaje.text = "No se pudo guardar el servicio. Tus datos siguen en el formulario."
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (lookupException: Exception) {
            botonGuardar.isEnabled = true
            mensaje.text = "No se pudo confirmar el motivo del error. Tus datos siguen en el formulario."
        }
    }

    private fun confirmarDesactivacion(servicio: CatalogoServicio) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val referencias = database.detalleCotizacionDao().obtenerPorServicioId(servicio.id).size
                val textoReferencias = if (referencias == 0) {
                    "No hay detalles de cotización asociados."
                } else {
                    "Se conservarán $referencias detalles y sus snapshots históricos."
                }
                MaterialAlertDialogBuilder(activity)
                    .setTitle("Desactivar servicio?")
                    .setMessage(
                        "Dejará de aparecer en nuevas cotizaciones. $textoReferencias"
                    )
                    .setPositiveButton("Confirmar desactivación") { _, _ ->
                        desactivarServicio(servicio.id)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarDialogoError(
                    "No se pudo consultar el historial",
                    "No se confirmó la desactivación. Intenta de nuevo."
                ) { confirmarDesactivacion(servicio) }
            }
        }
    }

    private fun desactivarServicio(id: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val actualizados = database.catalogoServicioDao()
                    .desactivar(id, Instant.now().toString())
                if (actualizados != 1) throw ErrorModulo("El servicio ya no está activo.")
                mostrarFicha(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarDialogoError(
                    "No se pudo desactivar el servicio",
                    mensajeError(exception, "El servicio se conserva sin cambios. Intenta de nuevo.")
                ) { desactivarServicio(id) }
            }
        }
    }

    private fun mostrarOrdenes(reiniciar: Boolean) {
        pantalla = Pantalla.ORDENES
        val contenido = crearContenido("Selecciona una orden")
        contenido.addView(
            texto("Se abrirá el borrador más reciente de la orden o se creará uno nuevo.", 14f, COLOR_SECUNDARIO),
            margenInferior()
        )
        contenido.addView(
            crearBuscador("Buscar por folio, cliente o equipo", filtroOrdenes) { valor ->
                filtroOrdenes = valor
                programarBusqueda { actualizarOrdenes(true) }
            },
            margenInferior()
        )
        estado = texto("Cargando órdenes…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar órdenes") {
            actualizarOrdenes(true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más órdenes", "Cargar más órdenes") {
            if (hayMas && !cargando) actualizarOrdenes(false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        if (reiniciar) desplazamiento = 0
        actualizarOrdenes(reiniciar)
    }

    private fun actualizarOrdenes(reiniciar: Boolean) {
        if (pantalla != Pantalla.ORDENES) return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            vistaLista.removeAllViews()
            estadoVista.text = "Cargando órdenes…"
            estadoVista.visibility = View.VISIBLE
            botonMas?.visibility = View.GONE
            botonReintentar?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasServicios.patronBusqueda(filtroOrdenes)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val pagina = database.ordenServicioDao().buscarParaCotizacion(
                    consulta = consulta,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val ordenes = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += ordenes.size
                ordenes.forEach { orden ->
                    val textoOrden = listOf(
                        orden.folio,
                        orden.clienteNombreSnapshot,
                        "${orden.marcaSnapshot} ${orden.modeloSnapshot}".trim(),
                        orden.estado
                    ).filter(String::isNotBlank).joinToString(" · ")
                    vistaLista.addView(
                        boton(textoOrden, "Crear o abrir borrador para ${orden.folio}") {
                            abrirBorrador(orden.id)
                        },
                        margenInferior()
                    )
                }
                estadoVista.text = when {
                    ordenes.isNotEmpty() -> ""
                    filtroOrdenes.isNotBlank() -> "No se encontraron órdenes. Prueba otra búsqueda."
                    else -> "No hay órdenes disponibles para cotizar. Registra una orden antes de crear un presupuesto."
                }
                estadoVista.visibility = if (ordenes.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = mensajeError(exception, "No se pudieron consultar las órdenes.")
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun abrirBorrador(ordenId: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorModulo("No se encontró la configuración del taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                val cotizacion = database.withTransaction {
                    val orden = database.ordenServicioDao().obtenerPorId(ordenId)
                        ?.takeIf { it.eliminadaEn == null }
                        ?: throw ErrorModulo("La orden ya no está disponible.")
                    val borrador = database.cotizacionDao().obtenerBorradorMasReciente(orden.id)
                    if (borrador != null) {
                        borrador
                    } else {
                        val version = Math.addExact(
                            database.cotizacionDao().obtenerMaximaVersion(orden.id) ?: 0,
                            1
                        )
                        val nueva = Cotizacion(
                            id = EntidadId.nuevo(),
                            ordenId = orden.id,
                            numeroVersion = version,
                            subtotalMinor = 0,
                            descuentoMinor = 0,
                            impuestoMinor = 0,
                            totalMinor = 0,
                            monedaCodigo = configuracion.monedaCodigo,
                            estado = ESTADO_BORRADOR,
                            creadaEn = Instant.now().toString()
                        )
                        database.cotizacionDao().insertar(nueva)
                        database.cotizacionDao().obtenerPorId(nueva.id)
                            ?: error("No se pudo volver a consultar el borrador.")
                    }
                }
                mostrarCotizacion(cotizacion.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarDialogoError(
                    "No se pudo abrir la cotización",
                    mensajeError(exception, "No se pudo crear o consultar el borrador.")
                ) { abrirBorrador(ordenId) }
            }
        }
    }

    private fun mostrarCotizacion(cotizacionId: String) {
        pantalla = Pantalla.COTIZACION
        cotizacionActualId = cotizacionId
        val contenido = crearContenido("Borrador de cotización")
        contenido.addView(texto("Cargando cotización…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val cotizacion = database.cotizacionDao().obtenerPorId(cotizacionId)
                    ?: throw ErrorModulo("No se encontró la cotización.")
                val orden = database.ordenServicioDao().obtenerPorId(cotizacion.ordenId)
                    ?: throw ErrorModulo("No se encontró la orden relacionada.")
                val detalles = database.detalleCotizacionDao().obtenerPorCotizacionId(cotizacion.id)
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                if (pantalla != Pantalla.COTIZACION) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Cotización v${cotizacion.numeroVersion}")
                contenido.addView(
                    texto("${orden.folio} · ${orden.clienteNombreSnapshot}", 16f, COLOR_TEXTO),
                    margenInferior()
                )
                contenido.addView(texto("Estado: ${cotizacion.estado}", 14f, COLOR_SECUNDARIO))
                if (cotizacion.estado == ESTADO_BORRADOR) {
                    if (configuracion?.monedaCodigo == cotizacion.monedaCodigo) {
                        contenido.addView(
                            boton("Agregar servicio", "Seleccionar un servicio activo para esta cotización") {
                                mostrarServiciosParaCotizacion(cotizacion.id, true)
                            },
                            margenInferior()
                        )
                    } else {
                        contenido.addView(
                            texto(
                                "La moneda configurada cambió. No se pueden agregar precios nuevos a este borrador.",
                                14f,
                                COLOR_ALERTA
                            ),
                            margenInferior()
                        )
                    }
                }
                contenido.addView(texto("Conceptos", 18f, COLOR_TEXTO, true), margenSuperior())
                if (detalles.isEmpty()) {
                    contenido.addView(texto("Todavía no hay conceptos.", 15f, COLOR_SECUNDARIO), margenInferior())
                } else {
                    detalles.forEach { detalle ->
                        contenido.addView(
                            texto(
                                "${detalle.descripcionSnapshot}\nCantidad: ${detalle.cantidad} · " +
                                    "${ReglasServicios.formatearPrecio(detalle.precioUnitarioMinor, cotizacion.monedaCodigo)} c/u · " +
                                    "Importe: ${ReglasServicios.formatearPrecio(detalle.importeMinor, cotizacion.monedaCodigo)}",
                                15f,
                                COLOR_TEXTO
                            ),
                            margenInferior()
                        )
                    }
                }
                contenido.addView(texto("Subtotal: ${ReglasServicios.formatearPrecio(cotizacion.subtotalMinor, cotizacion.monedaCodigo)}", 15f, COLOR_TEXTO), margenSuperior())
                contenido.addView(texto("Descuento: ${ReglasServicios.formatearPrecio(cotizacion.descuentoMinor, cotizacion.monedaCodigo)}", 15f, COLOR_TEXTO))
                contenido.addView(texto("Impuesto: ${ReglasServicios.formatearPrecio(cotizacion.impuestoMinor, cotizacion.monedaCodigo)}", 15f, COLOR_TEXTO))
                contenido.addView(texto("Total: ${ReglasServicios.formatearPrecio(cotizacion.totalMinor, cotizacion.monedaCodigo)}", 18f, COLOR_TEXTO, true), margenInferior())
                contenido.addView(
                    texto("Esta integración guarda un borrador y sus importes; envío, autorización y cierre pertenecen al flujo de cotizaciones.",
                        13f,
                        COLOR_SECUNDARIO
                    ),
                    margenSuperior()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.COTIZACION) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Borrador de cotización")
                contenido.addView(texto(mensajeError(exception, "No se pudo consultar la cotización."), 15f, COLOR_ERROR), margenInferior())
                contenido.addView(boton("Reintentar", "Volver a consultar la cotización") {
                    mostrarCotizacion(cotizacionId)
                })
            }
        }
    }

    private fun mostrarServiciosParaCotizacion(cotizacionId: String, reiniciar: Boolean) {
        pantalla = Pantalla.SERVICIOS_COTIZACION
        cotizacionActualId = cotizacionId
        val contenido = crearContenido("Agregar servicio")
        contenido.addView(
            texto("Solo se muestran servicios activos. El precio y la descripción se copiarán a la cotización.",
                14f,
                COLOR_SECUNDARIO
            ),
            margenInferior()
        )
        contenido.addView(
            crearBuscador("Buscar servicio activo", filtroSelectorServicios) { valor ->
                filtroSelectorServicios = valor
                programarBusqueda { actualizarServiciosParaCotizacion(true) }
            },
            margenInferior()
        )
        estado = texto("Cargando servicios…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar servicios activos") {
            actualizarServiciosParaCotizacion(true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más servicios", "Cargar más servicios activos") {
            if (hayMas && !cargando) actualizarServiciosParaCotizacion(false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        if (reiniciar) desplazamiento = 0
        actualizarServiciosParaCotizacion(reiniciar)
    }

    private fun actualizarServiciosParaCotizacion(reiniciar: Boolean) {
        if (pantalla != Pantalla.SERVICIOS_COTIZACION) return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            vistaLista.removeAllViews()
            estadoVista.text = "Cargando servicios…"
            estadoVista.visibility = View.VISIBLE
            botonMas?.visibility = View.GONE
            botonReintentar?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasServicios.patronBusqueda(filtroSelectorServicios)
        val offset = desplazamiento
        val quoteId = cotizacionActualId ?: return
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val cotizacion = database.cotizacionDao().obtenerPorId(quoteId)
                    ?: throw ErrorModulo("No se encontró la cotización.")
                val pagina = database.catalogoServicioDao().buscarActivosParaCotizacion(
                    consulta = consulta,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val servicios = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += servicios.size
                servicios.forEach { servicio ->
                    vistaLista.addView(
                        boton(
                            "${servicio.nombre} · ${ReglasServicios.formatearPrecio(servicio.precioBaseMinor, cotizacion.monedaCodigo)}",
                            "Agregar ${servicio.nombre} a la cotización"
                        ) { agregarServicioACotizacion(quoteId, servicio.id) },
                        margenInferior()
                    )
                }
                estadoVista.text = if (servicios.isEmpty()) {
                    if (filtroSelectorServicios.isBlank()) "No hay servicios activos disponibles." else "No hay servicios que coincidan."
                } else {
                    ""
                }
                estadoVista.visibility = if (servicios.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = mensajeError(exception, "No se pudieron consultar servicios activos.")
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun agregarServicioACotizacion(cotizacionId: String, servicioId: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                database.withTransaction {
                    val quote = database.cotizacionDao().obtenerPorId(cotizacionId)
                        ?.takeIf { it.estado == ESTADO_BORRADOR }
                        ?: throw ErrorModulo("La cotización ya no está en borrador.")
                    val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                        ?: throw ErrorModulo("No se encontró la configuración del taller.")
                    if (quote.monedaCodigo != configuracion.monedaCodigo) {
                        throw ErrorModulo("La moneda configurada cambió; no se agregó el servicio.")
                    }
                    val servicio = database.catalogoServicioDao().obtenerPorId(servicioId)
                        ?.takeIf(CatalogoServicio::activo)
                        ?: throw ErrorModulo("El servicio ya no está activo.")
                    val detalle = DetalleCotizacion(
                        id = EntidadId.nuevo(),
                        cotizacionId = quote.id,
                        servicioId = servicio.id,
                        tipo = TIPO_DETALLE_SERVICIO,
                        descripcionSnapshot = "${servicio.nombre}\n${servicio.descripcion}",
                        cantidad = 1,
                        precioUnitarioMinor = servicio.precioBaseMinor,
                        importeMinor = servicio.precioBaseMinor
                    )
                    database.detalleCotizacionDao().insertar(detalle)
                    val detalles = database.detalleCotizacionDao().obtenerPorCotizacionId(quote.id)
                    val totales = ReglasServicios.calcularTotales(
                        detalles = detalles,
                        descuentoMinor = quote.descuentoMinor,
                        impuestoTasa = configuracion.impuestoTasa
                    )
                    val actualizados = database.cotizacionDao().actualizarTotalesBorrador(
                        id = quote.id,
                        subtotalMinor = totales.subtotalMinor,
                        impuestoMinor = totales.impuestoMinor,
                        totalMinor = totales.totalMinor
                    )
                    if (actualizados != 1) throw ErrorModulo("No se pudieron actualizar los totales del borrador.")
                }
                filtroSelectorServicios = ""
                mostrarCotizacion(cotizacionId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarDialogoError(
                    "No se agregó el servicio",
                    mensajeError(exception, "No se pudo actualizar el borrador. No se confirmaron cambios.")
                ) { mostrarServiciosParaCotizacion(cotizacionId, true) }
            }
        }
    }

    private fun textoFilaServicio(servicio: CatalogoServicio, monedaCodigo: String): String = buildString {
        append(servicio.nombre)
        append("\n")
        append(servicio.codigo ?: "Sin código")
        append(" · ")
        append(ReglasServicios.formatearPrecio(servicio.precioBaseMinor, monedaCodigo))
        if (!servicio.activo) append(" · Inactivo")
    }

    private fun mensajeError(exception: Exception, predeterminado: String): String =
        if (exception is ErrorModulo) exception.mensajeUsuario else predeterminado

    private fun crearContenido(titulo: String): LinearLayout {
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
        agregarEncabezado(contenido, titulo)
        return contenido
    }

    private fun agregarEncabezado(contenido: LinearLayout, titulo: String) {
        val encabezado = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        encabezado.addView(
            texto(titulo, 22f, COLOR_TEXTO, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        encabezado.addView(boton("Atrás", "Volver a la pantalla anterior") { regresar() })
        encabezado.addView(boton("Bloquear", "Bloquear la aplicación") {
            cerrar()
            alBloquear()
        })
        contenido.addView(encabezado, margenInferior())
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
            } else {
                setSingleLine(true)
            }
            setText(valor)
        }
        layout.addView(campo)
        contenido.addView(layout, margenInferior())
        layouts[clave] = layout
        campos[clave] = campo
    }

    private fun crearBuscador(
        hint: String,
        textoInicial: String,
        alCambiar: (String) -> Unit
    ): TextInputLayout {
        val layout = TextInputLayout(activity).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        }
        val campo = TextInputEditText(layout.context).apply {
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            contentDescription = hint
            setText(textoInicial)
        }
        layout.addView(campo)
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                alCambiar(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        return layout
    }

    private fun programarBusqueda(accion: () -> Unit) {
        recargarBusqueda = accion
        handler.removeCallbacks(busquedaPendiente)
        handler.postDelayed(busquedaPendiente, BUSQUEDA_DEBOUNCE_MS)
    }

    private fun boton(titulo: String, descripcion: String, accion: () -> Unit): MaterialButton =
        MaterialButton(activity).apply {
            text = titulo
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            contentDescription = descripcion
            setOnClickListener { accion() }
        }

    private fun texto(
        valor: String,
        tamano: Float,
        color: Int,
        negrita: Boolean = false
    ): TextView = TextView(activity).apply {
        text = valor
        textSize = tamano
        setTextColor(color)
        if (negrita) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun mostrarDialogoError(
        titulo: String,
        mensaje: String,
        reintentar: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(if (reintentar == null) "Cerrar" else "Reintentar") { _, _ ->
                reintentar?.invoke()
            }
            .setNegativeButton(if (reintentar == null) null else "Cerrar", null)
            .show()
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
        private const val BUSQUEDA_DEBOUNCE_MS = 300L
        private const val ESTADO_BORRADOR = "BORRADOR"
        private const val TIPO_DETALLE_SERVICIO = "MANO_DE_OBRA"
        private val COLOR_TEXTO = Color.rgb(20, 32, 48)
        private val COLOR_SECUNDARIO = Color.rgb(91, 105, 122)
        private val COLOR_ERROR = Color.rgb(151, 55, 20)
        private val COLOR_ALERTA = Color.rgb(151, 55, 20)
    }
}
