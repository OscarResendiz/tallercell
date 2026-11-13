package com.example.tallercelulares.Modulos.Catalogos.Piezas

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
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
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.Cotizacion
import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacion
import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventario
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio
import com.example.tallercelulares.DataBase.Entidades.Pieza.Pieza
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.Modulos.Catalogos.Servicios.ReglasServicios
import com.example.tallercelulares.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Currency

internal class CatalogoPiezas(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val alVolverAlTablero: () -> Unit
) {
    private enum class Pantalla {
        CATALOGO, FICHA, FORMULARIO, MOVIMIENTO, ORDENES_COTIZACION, CERRADA
    }

    private data class Opcion(
        val id: String?,
        val etiqueta: String,
        val movimiento: MovimientoInventario? = null
    ) {
        override fun toString(): String = etiqueta
    }

    private data class Campo(
        val layout: TextInputLayout,
        val input: TextInputEditText
    )

    private val repositorio = RepositorioInventario(database)
    private val handler = Handler(Looper.getMainLooper())
    private var tarea: Job? = null
    private var recargarBusqueda: (() -> Unit)? = null
    private val busquedaPendiente = Runnable { recargarBusqueda?.invoke() }
    private val callbackAtras = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            regresar()
        }
    }

    private var pantalla = Pantalla.CATALOGO
    private var piezaActualId: String? = null
    private var filtroCatalogo = ""
    private var filtroOrdenes = ""
    private var incluirInactivas = false
    private var desplazamiento = 0
    private var desplazamientoMovimientos = 0
    private var hayMas = false
    private var hayMasMovimientos = false
    private var cargando = false
    private var estado: TextView? = null
    private var lista: LinearLayout? = null
    private var botonMas: MaterialButton? = null
    private var botonMasMovimientos: MaterialButton? = null
    private var botonReintentar: MaterialButton? = null

    init {
        activity.onBackPressedDispatcher.addCallback(activity, callbackAtras)
        mostrarCatalogo(true)
    }

    fun refrescar() {
        when (pantalla) {
            Pantalla.CATALOGO -> actualizarCatalogo(true)
            Pantalla.FICHA -> piezaActualId?.let(::mostrarFicha)
            Pantalla.ORDENES_COTIZACION -> mostrarOrdenesParaCotizacion(piezaActualId, true)
            Pantalla.FORMULARIO, Pantalla.MOVIMIENTO, Pantalla.CERRADA -> Unit
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
            Pantalla.FORMULARIO, Pantalla.MOVIMIENTO ->
                piezaActualId?.let(::mostrarFicha) ?: mostrarCatalogo(true)
            Pantalla.ORDENES_COTIZACION ->
                piezaActualId?.let(::mostrarFicha) ?: mostrarCatalogo(true)
            Pantalla.CERRADA -> Unit
        }
    }

    private fun mostrarCatalogo(reiniciar: Boolean) {
        pantalla = Pantalla.CATALOGO
        piezaActualId = null
        val contenido = crearContenido("Inventario de piezas")
        contenido.addView(
            boton("Registrar pieza", "Crear una pieza en el catálogo") {
                mostrarFormularioPieza(null)
            },
            margenInferior()
        )
        lateinit var botonFiltro: MaterialButton
        botonFiltro = boton(
            if (incluirInactivas) "Ocultar piezas inactivas" else "Mostrar piezas inactivas",
            "Cambiar la visibilidad de las piezas inactivas"
        ) {
            incluirInactivas = !incluirInactivas
            botonFiltro.text =
                if (incluirInactivas) "Ocultar piezas inactivas" else "Mostrar piezas inactivas"
            actualizarCatalogo(true)
        }
        contenido.addView(botonFiltro, margenInferior())
        contenido.addView(
            crearBuscador(
                "Buscar por nombre, variante, código o número de parte",
                filtroCatalogo
            ) {
                filtroCatalogo = it
                programarBusqueda { actualizarCatalogo(true) }
            },
            margenInferior()
        )
        estado = texto("Cargando piezas…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar piezas") {
            actualizarCatalogo(true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más piezas", "Cargar más piezas") {
            if (hayMas && !cargando) actualizarCatalogo(false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        if (reiniciar) desplazamiento = 0
        actualizarCatalogo(reiniciar)
    }

    private fun actualizarCatalogo(reiniciar: Boolean) {
        if (pantalla != Pantalla.CATALOGO) return
        val estadoVista = estado ?: return
        val listaVista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            listaVista.removeAllViews()
            estadoVista.text = activity.getString(R.string.inventory_loading_pieces)
            estadoVista.visibility = View.VISIBLE
            botonMas?.visibility = View.GONE
            botonReintentar?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasPiezas.normalizarBusqueda(filtroCatalogo)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorInventario("No se encontró la configuración del taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                val pagina = database.piezaDao().buscar(
                    consulta = consulta,
                    incluirInactivas = incluirInactivas,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val piezas = pagina.take(PAGE_SIZE)
                val saldos = repositorio.saldos(piezas.map(Pieza::id))
                hayMas = pagina.size > PAGE_SIZE
                desplazamiento += piezas.size
                piezas.forEach { pieza ->
                    val saldo = saldos[pieza.id]
                    listaVista.addView(
                        boton(
                            textoFilaPieza(pieza, saldo, configuracion.monedaCodigo),
                            "Abrir pieza ${pieza.nombre}"
                        ) { mostrarFicha(pieza.id) },
                        margenInferior()
                    )
                }
                estadoVista.text = when {
                    piezas.isNotEmpty() -> ""
                    filtroCatalogo.isNotBlank() -> "No se encontraron piezas. Prueba otra búsqueda."
                    incluirInactivas -> "No hay piezas registradas."
                    else -> "No hay piezas activas. Puedes registrar la primera."
                }
                estadoVista.visibility = if (piezas.isEmpty()) View.VISIBLE else View.GONE
                botonMas?.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonReintentar?.visibility = View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = mensajeError(exception, "No se pudieron consultar las piezas.")
                estadoVista.visibility = View.VISIBLE
                botonReintentar?.visibility = View.VISIBLE
            } finally {
                cargando = false
            }
        }
    }

    private fun mostrarFicha(piezaId: String) {
        pantalla = Pantalla.FICHA
        piezaActualId = piezaId
        desplazamientoMovimientos = 0
        hayMasMovimientos = false
        botonMasMovimientos = null
        val contenido = crearContenido("Ficha de pieza")
        contenido.addView(texto("Cargando pieza…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val pieza = database.piezaDao().obtenerPorId(piezaId)
                    ?: throw ErrorInventario("No se encontró la pieza.")
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorInventario("No se encontró la configuración del taller.")
                val saldo = try {
                    repositorio.saldo(pieza.id)
                } catch (exception: ErrorInventario) {
                    null
                }
                val paginaMovimientos = database.movimientoInventarioDao()
                    .obtenerPaginaPorPiezaId(pieza.id, PAGE_SIZE + 1, 0)
                val movimientos = paginaMovimientos.take(PAGE_SIZE)
                    .sortedWith(compareByDescending<MovimientoInventario> {
                        it.confirmadaEn ?: it.creadoEn
                    }.thenByDescending { it.id })
                hayMasMovimientos = paginaMovimientos.size > PAGE_SIZE
                desplazamientoMovimientos = movimientos.size
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                contenido.addView(texto(pieza.nombre, 22f, COLOR_PRIMARIO, true))
                contenido.addView(
                    texto(
                        "${pieza.variante} · ${pieza.calidad}\n" +
                            "Código: ${pieza.codigo ?: "Sin código"}\n" +
                            "Número de parte: ${pieza.numeroParte ?: "Sin número de parte"}\n" +
                            "Costo: ${ReglasServicios.formatearPrecio(pieza.costoUnitarioMinor, configuracion.monedaCodigo)}\n" +
                            "Precio de venta: ${ReglasServicios.formatearPrecio(pieza.precioVentaMinor, configuracion.monedaCodigo)}\n" +
                            "Estado: ${if (pieza.activo) "Activa" else "Inactiva"}",
                        15f,
                        COLOR_TEXTO
                    ),
                    margenInferior()
                )
                contenido.addView(texto("Inventario", 18f, COLOR_TEXTO, true), margenSuperior())
                if (saldo == null) {
                    contenido.addView(
                        texto(
                            "Saldo requiere revisión. El historial se conserva, pero no se permiten operaciones que dependan de un cálculo ambiguo.",
                            15f,
                            COLOR_ALERTA
                        ),
                        margenInferior()
                    )
                } else {
                    contenido.addView(
                        texto(
                            "Existencia física: ${saldo.existenciaFisica}\n" +
                                "Reservada: ${saldo.cantidadReservada}\n" +
                                "Disponible: ${saldo.disponible}\n" +
                                "Mínimo: ${pieza.existenciaMinima}" +
                                if (saldo.disponible < pieza.existenciaMinima) "\nPor debajo del mínimo" else "",
                            16f,
                            if (saldo.disponible < pieza.existenciaMinima) COLOR_ALERTA else COLOR_TEXTO
                        ),
                        margenInferior()
                    )
                }
                if (pieza.activo) {
                    contenido.addView(
                        boton("Editar pieza", "Editar datos de la pieza") {
                            mostrarFormularioPieza(pieza.id)
                        },
                        margenInferior()
                    )
                    contenido.addView(
                        boton("Agregar a cotización", "Seleccionar una orden para cotizar esta pieza") {
                            mostrarOrdenesParaCotizacion(pieza.id, true)
                        },
                        margenInferior()
                    )
                    if (saldo != null) {
                        agregarAccionesMovimiento(contenido, pieza.id)
                        contenido.addView(
                            boton("Desactivar pieza", "Desactivar sin borrar el historial") {
                                confirmarDesactivacion(pieza)
                            },
                            margenInferior()
                        )
                    }
                } else {
                    contenido.addView(texto("La pieza inactiva se conserva para el historial.", 14f, COLOR_SECUNDARIO))
                }
                contenido.addView(texto("Movimientos", 18f, COLOR_TEXTO, true), margenSuperior())
                if (movimientos.isEmpty()) {
                    contenido.addView(texto("Todavía no hay movimientos registrados.", 15f, COLOR_SECUNDARIO))
                } else {
                    movimientos.forEach { movimiento ->
                        agregarFilaMovimiento(contenido, movimiento)
                    }
                    botonMasMovimientos = boton(
                        "Cargar más movimientos",
                        "Consultar movimientos anteriores de la pieza"
                    ) {
                        if (hayMasMovimientos) cargarMasMovimientos(pieza.id, contenido)
                    }.apply {
                        visibility = if (hayMasMovimientos) View.VISIBLE else View.GONE
                    }
                    contenido.addView(botonMasMovimientos, margenInferior())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FICHA) return@launch
                contenido.removeAllViews()
                contenido.addView(texto(mensajeError(exception, "No se pudo consultar la ficha."), 15f, COLOR_ERROR))
                contenido.addView(
                    boton("Reintentar", "Volver a consultar la pieza") { mostrarFicha(piezaId) },
                    margenInferior()
                )
            }
        }
    }

    private fun agregarAccionesMovimiento(contenido: LinearLayout, piezaId: String) {
        contenido.addView(texto("Operaciones de inventario", 18f, COLOR_TEXTO, true), margenSuperior())
        listOf(
            TipoMovimientoInventario.ENTRADA to "Registrar entrada",
            TipoMovimientoInventario.RESERVA to "Reservar para una orden",
            TipoMovimientoInventario.LIBERACION to "Liberar reserva",
            TipoMovimientoInventario.CONSUMO to "Registrar consumo",
            TipoMovimientoInventario.DEVOLUCION to "Registrar devolución",
            TipoMovimientoInventario.AJUSTE to "Ajustar existencia"
        ).forEach { (tipo, titulo) ->
            contenido.addView(
                boton(titulo, titulo) { mostrarFormularioMovimiento(piezaId, tipo) },
                margenInferior()
            )
        }
    }

    private fun agregarFilaMovimiento(
        contenido: LinearLayout,
        movimiento: MovimientoInventario
    ) {
        val estadoMovimiento = if (movimiento.confirmadaEn == null) "Pendiente" else "Confirmado"
        val sentido = movimiento.sentidoAjuste?.let { " · $it" }.orEmpty()
        val orden = movimiento.ordenId?.let { " · Orden: $it" }.orEmpty()
        val origen = movimiento.movimientoOrigenId?.let { "\nOrigen: $it" }.orEmpty()
        val fila = texto(
            "${etiquetaTipo(movimiento.tipo)}$sentido · ${movimiento.cantidad} · $estadoMovimiento\n" +
                "Fecha: ${movimiento.confirmadaEn ?: movimiento.creadoEn}" +
                (movimiento.motivo?.let { "\nMotivo: $it" } ?: "") +
                orden + origen,
            14f,
            COLOR_TEXTO
        )
        contenido.addView(fila, margenInferior())
        if (movimiento.confirmadaEn != null) {
            contenido.addView(
                boton("Corregir movimiento", "Registrar una compensación vinculada al movimiento") {
                    mostrarDialogoCompensacion(movimiento)
                },
                margenInferior()
            )
        }
    }

    private fun cargarMasMovimientos(piezaId: String, contenido: LinearLayout) {
        if (pantalla != Pantalla.FICHA || cargando || !hayMasMovimientos) return
        val offset = desplazamientoMovimientos
        cargando = true
        botonMasMovimientos?.isEnabled = false
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val pagina = database.movimientoInventarioDao()
                    .obtenerPaginaPorPiezaId(piezaId, PAGE_SIZE + 1, offset)
                val nuevos = pagina.take(PAGE_SIZE)
                    .sortedWith(compareByDescending<MovimientoInventario> {
                        it.confirmadaEn ?: it.creadoEn
                    }.thenByDescending { it.id })
                nuevos.forEach { agregarFilaMovimiento(contenido, it) }
                desplazamientoMovimientos += nuevos.size
                hayMasMovimientos = pagina.size > PAGE_SIZE
                botonMasMovimientos?.visibility =
                    if (hayMasMovimientos) View.VISIBLE else View.GONE
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarError(
                    "No se pudieron consultar más movimientos",
                    mensajeError(exception, "El historial no se modificó.")
                )
            } finally {
                cargando = false
                botonMasMovimientos?.isEnabled = true
            }
        }
    }

    private fun mostrarFormularioPieza(piezaId: String?) {
        pantalla = Pantalla.FORMULARIO
        piezaActualId = piezaId
        val contenido = crearContenido(if (piezaId == null) "Registrar pieza" else "Editar pieza")
        contenido.addView(texto("Cargando formulario…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorInventario("No se encontró la configuración del taller.")
                Currency.getInstance(configuracion.monedaCodigo)
                val pieza = piezaId?.let { database.piezaDao().obtenerPorId(it) }
                if (piezaId != null && (pieza == null || !pieza.activo)) {
                    throw ErrorInventario("La pieza ya no está disponible para editar.")
                }
                if (pantalla != Pantalla.FORMULARIO) return@launch
                contenido.removeAllViews()
                contenido.addView(
                    texto("Los cambios no modifican movimientos ni cotizaciones anteriores.", 14f, COLOR_SECUNDARIO),
                    margenInferior()
                )
                contenido.addView(texto("Moneda: ${configuracion.monedaCodigo}", 14f, COLOR_SECUNDARIO))
                val codigo = agregarCampo(contenido, "Código (opcional)", pieza?.codigo.orEmpty())
                val numeroParte = agregarCampo(
                    contenido, "Número de parte (opcional)", pieza?.numeroParte.orEmpty()
                )
                val nombre = agregarCampo(contenido, "Nombre", pieza?.nombre.orEmpty())
                val variante = agregarCampo(contenido, "Variante", pieza?.variante.orEmpty())
                val calidad = agregarCampo(contenido, "Calidad", pieza?.calidad.orEmpty())
                val costo = agregarCampo(
                    contenido,
                    "Costo unitario",
                    pieza?.let { ReglasPiezas.textoPrecio(it.costoUnitarioMinor, configuracion.monedaCodigo) }
                        .orEmpty(),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                )
                val precio = agregarCampo(
                    contenido,
                    "Precio de venta",
                    pieza?.let { ReglasPiezas.textoPrecio(it.precioVentaMinor, configuracion.monedaCodigo) }
                        .orEmpty(),
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                )
                val minimo = agregarCampo(
                    contenido,
                    "Existencia mínima",
                    pieza?.existenciaMinima?.toString().orEmpty(),
                    InputType.TYPE_CLASS_NUMBER
                )
                val errorGeneral = texto("", 14f, COLOR_ERROR)
                contenido.addView(errorGeneral, margenInferior())
                contenido.addView(
                    boton(if (pieza == null) "Guardar pieza" else "Guardar cambios", "Guardar los datos de la pieza") {
                        guardarPieza(
                            pieza,
                            configuracion.monedaCodigo,
                            CampoValor(
                                codigo, numeroParte, nombre, variante, calidad,
                                costo, precio, minimo
                            ),
                            errorGeneral
                        )
                    },
                    margenInferior()
                )
                contenido.addView(
                    boton("Cancelar", "Cancelar la edición") {
                        piezaId?.let(::mostrarFicha) ?: mostrarCatalogo(true)
                    },
                    margenInferior()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FORMULARIO) return@launch
                contenido.removeAllViews()
                contenido.addView(texto(mensajeError(exception, "No se pudo abrir el formulario."), 15f, COLOR_ERROR))
                contenido.addView(boton("Volver", "Volver a la ficha") {
                    piezaId?.let(::mostrarFicha) ?: mostrarCatalogo(true)
                })
            }
        }
    }

    private data class CampoValor(
        val codigo: Campo,
        val numeroParte: Campo,
        val nombre: Campo,
        val variante: Campo,
        val calidad: Campo,
        val costo: Campo,
        val precioVenta: Campo,
        val existenciaMinima: Campo
    ) {
        fun leer() = CamposPieza(
            codigo = codigo.input.text?.toString().orEmpty(),
            numeroParte = numeroParte.input.text?.toString().orEmpty(),
            nombre = nombre.input.text?.toString().orEmpty(),
            variante = variante.input.text?.toString().orEmpty(),
            calidad = calidad.input.text?.toString().orEmpty(),
            costo = costo.input.text?.toString().orEmpty(),
            precioVenta = precioVenta.input.text?.toString().orEmpty(),
            existenciaMinima = existenciaMinima.input.text?.toString().orEmpty()
        )
    }

    private fun guardarPieza(
        pieza: Pieza?,
        monedaCodigo: String,
        campos: CampoValor,
        errorGeneral: TextView
    ) {
        val valores = campos.leer()
        val errores = ReglasPiezas.validarPieza(valores, monedaCodigo)
        listOf(
            "codigo" to campos.codigo,
            "numeroParte" to campos.numeroParte,
            "nombre" to campos.nombre,
            "variante" to campos.variante,
            "calidad" to campos.calidad,
            "costo" to campos.costo,
            "precioVenta" to campos.precioVenta,
            "existenciaMinima" to campos.existenciaMinima
        ).forEach { (clave, campo) -> campo.layout.error = errores[clave] }
        errorGeneral.text = ""
        if (errores.isNotEmpty()) return

        val costoMinor = ReglasPiezas.precioMinor(valores.costo, monedaCodigo)
            ?: run {
                campos.costo.layout.error = "Escribe un costo válido para la moneda."
                return
            }
        val precioMinor = ReglasPiezas.precioMinor(valores.precioVenta, monedaCodigo)
            ?: run {
                campos.precioVenta.layout.error = "Escribe un precio válido para la moneda."
                return
            }
        val minimo = valores.existenciaMinima.trim().toIntOrNull()
            ?: run {
                campos.existenciaMinima.layout.error = "Escribe una cantidad entera no negativa."
                return
            }
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                database.withTransaction {
                    if (pieza == null) {
                        val nueva = Pieza(
                            id = EntidadId.nuevo(),
                            codigo = ReglasPiezas.normalizarOpcional(valores.codigo),
                            numeroParte = ReglasPiezas.normalizarOpcional(valores.numeroParte),
                            nombre = valores.nombre.trim(),
                            variante = valores.variante.trim(),
                            calidad = valores.calidad.trim(),
                            costoUnitarioMinor = costoMinor,
                            precioVentaMinor = precioMinor,
                            existenciaMinima = minimo,
                            activo = true,
                            creadoEn = ahora,
                            actualizadoEn = ahora
                        )
                        database.piezaDao().insertar(nueva)
                    } else {
                        val actualizados = database.piezaDao().actualizarDatos(
                            id = pieza.id,
                            codigo = ReglasPiezas.normalizarOpcional(valores.codigo),
                            numeroParte = ReglasPiezas.normalizarOpcional(valores.numeroParte),
                            nombre = valores.nombre.trim(),
                            variante = valores.variante.trim(),
                            calidad = valores.calidad.trim(),
                            costoUnitarioMinor = costoMinor,
                            precioVentaMinor = precioMinor,
                            existenciaMinima = minimo,
                            actualizadoEn = ahora
                        )
                        if (actualizados != 1) {
                            throw ErrorInventario("La pieza cambió o fue desactivada. Vuelve a consultar.")
                        }
                    }
                }
                if (pieza == null) mostrarCatalogo(true) else mostrarFicha(pieza.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                errorGeneral.text = mensajeError(exception, "No se pudieron guardar los cambios.")
            }
        }
    }

    private fun mostrarFormularioMovimiento(
        piezaId: String,
        tipo: TipoMovimientoInventario
    ) {
        pantalla = Pantalla.MOVIMIENTO
        piezaActualId = piezaId
        val contenido = crearContenido(etiquetaTipo(tipo.codigo))
        contenido.addView(texto("Cargando datos del movimiento…", 15f, COLOR_SECUNDARIO))
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val pieza = database.piezaDao().obtenerPorId(piezaId)
                    ?.takeIf { it.activo }
                    ?: throw ErrorInventario("La pieza ya no está activa.")
                val configuracion = database.configuracionTallerDao().obtenerPorId("TALLER")
                    ?: throw ErrorInventario("No se encontró la configuración del taller.")
                val movimientos = database.movimientoInventarioDao().obtenerPorPiezaId(pieza.id)
                val ordenes = database.ordenServicioDao()
                    .buscarParaCotizacion("", PAGE_SIZE, 0)
                    .map { Opcion(it.id, "${it.folio} · ${it.clienteNombreSnapshot}") }
                val opcionesOrigen = when (tipo) {
                    TipoMovimientoInventario.LIBERACION -> opcionesReserva(movimientos)
                    TipoMovimientoInventario.DEVOLUCION -> opcionesConsumo(movimientos)
                    TipoMovimientoInventario.CONSUMO -> listOf(Opcion(null, "Consumo directo"))
                        .plus(opcionesReserva(movimientos))
                    else -> emptyList()
                }
                if (pantalla != Pantalla.MOVIMIENTO) return@launch
                contenido.removeAllViews()
                val saldo = repositorio.saldo(pieza.id)
                contenido.addView(
                    texto(
                        "Disponible ahora: ${saldo.disponible} · Reservada: ${saldo.cantidadReservada}",
                        15f,
                        COLOR_SECUNDARIO
                    ),
                    margenInferior()
                )
                val cantidad = agregarCampo(
                    contenido,
                    "Cantidad",
                    tipoEntrada = InputType.TYPE_CLASS_NUMBER
                )
                val motivo = agregarCampo(contenido, "Motivo")
                val costo = if (tipo == TipoMovimientoInventario.ENTRADA) {
                    contenido.addView(texto("Costo de esta entrada (${configuracion.monedaCodigo}, opcional)", 14f, COLOR_SECUNDARIO))
                    agregarCampo(
                        contenido,
                        "Costo unitario opcional",
                        tipoEntrada = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    )
                } else null
                val selectorAjuste = if (tipo == TipoMovimientoInventario.AJUSTE) {
                    contenido.addView(texto("Sentido del ajuste", 14f, COLOR_TEXTO))
                    crearSpinner(
                        listOf(
                            Opcion(SentidoAjuste.INCREMENTO.codigo, "Incrementa existencia"),
                            Opcion(SentidoAjuste.DECREMENTO.codigo, "Disminuye existencia")
                        )
                    ).also { contenido.addView(it, margenInferior()) }
                } else null
                val selectorOrigen = if (opcionesOrigen.isNotEmpty() &&
                    (tipo == TipoMovimientoInventario.LIBERACION ||
                        tipo == TipoMovimientoInventario.DEVOLUCION ||
                        tipo == TipoMovimientoInventario.CONSUMO)
                ) {
                    contenido.addView(
                        texto(
                            when (tipo) {
                                TipoMovimientoInventario.LIBERACION -> "Reserva a liberar"
                                TipoMovimientoInventario.DEVOLUCION -> "Consumo que se devuelve"
                                else -> "Reserva (opcional)"
                            },
                            14f,
                            COLOR_TEXTO
                        )
                    )
                    crearSpinner(opcionesOrigen).also { contenido.addView(it, margenInferior()) }
                } else null
                val requiereOrden = tipo == TipoMovimientoInventario.RESERVA ||
                    tipo == TipoMovimientoInventario.CONSUMO
                val selectorOrden = if (requiereOrden) {
                    val opciones = if (tipo == TipoMovimientoInventario.RESERVA) {
                        ordenes
                    } else {
                        listOf(Opcion(null, "Sin orden")).plus(ordenes)
                    }
                    contenido.addView(texto("Orden relacionada", 14f, COLOR_TEXTO))
                    crearSpinner(opciones).also { contenido.addView(it, margenInferior()) }
                } else null
                if (tipo == TipoMovimientoInventario.CONSUMO &&
                    selectorOrigen != null &&
                    selectorOrden != null
                ) {
                    fun actualizarOrdenHabilitada() {
                        val opcion = selectorOrigen.selectedItem as? Opcion
                        selectorOrden.isEnabled = opcion?.id == null
                        if (opcion?.id != null && selectorOrden.adapter.count > 0) {
                            selectorOrden.setSelection(0)
                        }
                    }
                    selectorOrigen.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) = actualizarOrdenHabilitada()
                        }
                }
                val errorGeneral = texto("", 14f, COLOR_ERROR)
                contenido.addView(errorGeneral, margenInferior())
                contenido.addView(
                    boton("Revisar y confirmar", "Validar y confirmar el movimiento") {
                        val cantidadNumero = cantidad.input.text?.toString()?.trim()?.toIntOrNull()
                        val motivoTexto = motivo.input.text?.toString().orEmpty().trim()
                        val costoMinor = costo?.let {
                            val textoCosto = it.input.text?.toString().orEmpty().trim()
                            if (textoCosto.isEmpty()) null else {
                                ReglasPiezas.precioMinor(
                                    textoCosto,
                                    configuracion.monedaCodigo
                                )
                            }
                        }
                        if (cantidadNumero == null || cantidadNumero <= 0) {
                            cantidad.layout.error = "Escribe un entero positivo."
                            return@boton
                        }
                        cantidad.layout.error = null
                        if (motivoTexto.isBlank()) {
                            motivo.layout.error = "El motivo es obligatorio."
                            return@boton
                        }
                        motivo.layout.error = null
                        if (costo != null &&
                            costo.input.text?.isNotBlank() == true &&
                            costoMinor == null
                        ) {
                            costo.layout.error = "Escribe un importe válido para la moneda."
                            return@boton
                        }
                        costo?.layout?.error = null
                        val opcionOrigen = selectorOrigen?.selectedItem as? Opcion
                        val opcionOrden = selectorOrden?.selectedItem as? Opcion
                        val sentido = selectorAjuste?.selectedItem as? Opcion
                        val solicitud = SolicitudMovimiento(
                            piezaId = pieza.id,
                            tipo = tipo,
                            cantidad = cantidadNumero,
                            motivo = motivoTexto,
                            ordenId = when {
                                tipo == TipoMovimientoInventario.CONSUMO &&
                                    opcionOrigen?.id != null -> null
                                else -> opcionOrden?.id
                            },
                            costoUnitarioMinor = costoMinor,
                            sentidoAjuste = sentido?.id?.let { codigo ->
                                SentidoAjuste.entries.firstOrNull { it.codigo == codigo }
                            },
                            movimientoOrigenId = when (tipo) {
                                TipoMovimientoInventario.LIBERACION,
                                TipoMovimientoInventario.DEVOLUCION -> opcionOrigen?.id
                                TipoMovimientoInventario.CONSUMO -> opcionOrigen?.id
                                else -> null
                            }
                        )
                        val ordenEtiqueta = selectorOrden?.selectedItem?.toString()
                            ?.takeUnless { it == "Sin orden" }
                        val origenEtiqueta = selectorOrigen?.selectedItem?.toString()
                        val resumen = buildString {
                            append("${etiquetaTipo(tipo.codigo)} · ${solicitud.cantidad} pieza(s)")
                            if (ordenEtiqueta != null) append("\n$ordenEtiqueta")
                            if (origenEtiqueta != null && origenEtiqueta != "Consumo directo") {
                                append("\n$origenEtiqueta")
                            }
                            append("\nMotivo: ${solicitud.motivo}")
                        }
                        MaterialAlertDialogBuilder(activity)
                            .setTitle("Confirmar movimiento")
                            .setMessage(resumen)
                            .setNegativeButton("Editar", null)
                            .setPositiveButton("Confirmar") { _, _ ->
                                confirmarMovimiento(solicitud, errorGeneral)
                            }
                            .show()
                    },
                    margenInferior()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.MOVIMIENTO) return@launch
                contenido.removeAllViews()
                contenido.addView(texto(mensajeError(exception, "No se pudo abrir el movimiento."), 15f, COLOR_ERROR))
                contenido.addView(boton("Volver", "Volver a la ficha") { mostrarFicha(piezaId) })
            }
        }
    }

    private fun confirmarMovimiento(
        solicitud: SolicitudMovimiento,
        errorGeneral: TextView
    ) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                repositorio.registrar(solicitud)
                mostrarFicha(solicitud.piezaId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                errorGeneral.text = mensajeError(exception, "No se pudo confirmar el movimiento.")
            }
        }
    }

    private fun opcionesReserva(movimientos: List<MovimientoInventario>): List<Opcion> {
        if (movimientos.any {
                it.confirmadaEn != null &&
                (it.tipo == TipoMovimientoInventario.LIBERACION.codigo ||
                    it.tipo == TipoMovimientoInventario.CONSUMO.codigo) &&
                it.movimientoOrigenId == null
            }
        ) {
            throw ErrorInventario(
                "Hay liberaciones históricas sin vínculo a una reserva. No es seguro liberar otra reserva desde esta ficha."
            )
        }
        val reservas = movimientos.filter {
            it.tipo == TipoMovimientoInventario.RESERVA.codigo && it.confirmadaEn != null
        }
        return reservas.mapNotNull { reserva ->
            val remanente = try {
                ReglasPiezas.remanenteReserva(reserva.id, movimientos)
            } catch (exception: Exception) {
                throw ErrorInventario(
                    mensajeError(exception, "No se pudo determinar el saldo de la reserva.")
                )
            }
            if (remanente <= 0) null
            else Opcion(reserva.id, "${reserva.ordenId ?: "Sin orden"} · Restan $remanente", reserva)
        }
    }

    private fun opcionesConsumo(movimientos: List<MovimientoInventario>): List<Opcion> =
        movimientos.asSequence()
            .filter {
                it.tipo == TipoMovimientoInventario.CONSUMO.codigo && it.confirmadaEn != null
            }
            .mapNotNull { consumo ->
                val devuelto = movimientos.asSequence()
                    .filter {
                        it.tipo == TipoMovimientoInventario.DEVOLUCION.codigo &&
                            it.confirmadaEn != null &&
                            it.movimientoOrigenId == consumo.id
                    }
                    .sumOf { it.cantidad.toLong() }
                val remanente = consumo.cantidad.toLong() - devuelto
                if (remanente <= 0) null
                else Opcion(
                    consumo.id,
                    "${consumo.ordenId ?: "Sin orden"} · Restan $remanente",
                    consumo
                )
            }
            .toList()

    private fun confirmarDesactivacion(pieza: Pieza) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Desactivar ${pieza.nombre}")
            .setMessage(
                "La pieza dejará de estar disponible para nuevas operaciones y cotizaciones. " +
                    "Sus movimientos y referencias históricas se conservarán."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Desactivar") { _, _ ->
                tarea?.cancel()
                tarea = activity.lifecycleScope.launch {
                    try {
                        val saldo = repositorio.saldo(pieza.id)
                        if (saldo.cantidadReservada > 0) {
                            throw ErrorInventario(
                                "Libera o consume las reservas pendientes antes de desactivar la pieza."
                            )
                        }
                        val cantidad = database.piezaDao().desactivar(
                            pieza.id,
                            Instant.now().toString()
                        )
                        if (cantidad != 1) {
                            throw ErrorInventario("La pieza ya fue desactivada. Actualiza la ficha.")
                        }
                        mostrarFicha(pieza.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (exception: Exception) {
                        mostrarError(
                            "No se pudo desactivar la pieza",
                            mensajeError(exception, "No se pudo completar la baja lógica.")
                        ) { mostrarFicha(pieza.id) }
                    }
                }
            }
            .show()
    }

    private fun mostrarDialogoCompensacion(movimiento: MovimientoInventario) {
        val contenedor = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val campo = agregarCampo(contenedor, "Motivo de corrección")
        MaterialAlertDialogBuilder(activity)
            .setTitle("Registrar compensación")
            .setMessage(
                "Se conservará el movimiento original. La compensación solo se confirmará " +
                    "si el saldo permite revertir la operación completa."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val motivo = campo.input.text?.toString().orEmpty()
                        if (motivo.isBlank()) {
                            campo.layout.error = "El motivo es obligatorio."
                            return@setOnClickListener
                        }
                        campo.layout.error = null
                        dialog.dismiss()
                        tarea?.cancel()
                        tarea = activity.lifecycleScope.launch {
                            try {
                                repositorio.compensar(movimiento.id, motivo)
                                mostrarFicha(movimiento.piezaId)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (exception: Exception) {
                                mostrarError(
                                    "No se pudo corregir el movimiento",
                                    mensajeError(exception, "No se registró la compensación.")
                                ) { mostrarFicha(movimiento.piezaId) }
                            }
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun mostrarOrdenesParaCotizacion(piezaId: String?, reiniciar: Boolean) {
        val id = piezaId ?: return
        pantalla = Pantalla.ORDENES_COTIZACION
        piezaActualId = id
        val contenido = crearContenido("Agregar pieza a cotización")
        contenido.addView(
            texto(
                "Selecciona una orden. El precio y la descripción se copiarán a su borrador; " +
                    "esta acción no reserva ni consume existencias.",
                14f,
                COLOR_SECUNDARIO
            ),
            margenInferior()
        )
        contenido.addView(
            crearBuscador("Buscar por folio, cliente o equipo", filtroOrdenes) {
                filtroOrdenes = it
                programarBusqueda { mostrarOrdenesParaCotizacion(id, true) }
            },
            margenInferior()
        )
        estado = texto("Cargando órdenes…", 15f, COLOR_SECUNDARIO)
        contenido.addView(estado)
        botonReintentar = boton("Reintentar", "Volver a consultar las órdenes") {
            mostrarOrdenesParaCotizacion(id, true)
        }.apply { visibility = View.GONE }
        contenido.addView(botonReintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más órdenes", "Cargar más órdenes") {
            if (hayMas && !cargando) cargarOrdenesCotizacion(id, false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)
        if (reiniciar) desplazamiento = 0
        cargarOrdenesCotizacion(id, reiniciar)
    }

    private fun cargarOrdenesCotizacion(piezaId: String, reiniciar: Boolean) {
        val estadoVista = estado ?: return
        val listaVista = lista ?: return
        if (reiniciar) {
            desplazamiento = 0
            listaVista.removeAllViews()
            estadoVista.text = activity.getString(R.string.inventory_loading_orders)
            estadoVista.visibility = View.VISIBLE
            botonReintentar?.visibility = View.GONE
            botonMas?.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasPiezas.normalizarBusqueda(filtroOrdenes)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val ordenes = database.ordenServicioDao().buscarParaCotizacion(
                    consulta,
                    PAGE_SIZE + 1,
                    offset
                )
                val pagina = ordenes.take(PAGE_SIZE)
                hayMas = ordenes.size > PAGE_SIZE
                desplazamiento += pagina.size
                pagina.forEach { orden ->
                    listaVista.addView(
                        boton(
                            "${orden.folio} · ${orden.clienteNombreSnapshot}\n" +
                                "${orden.marcaSnapshot} ${orden.modeloSnapshot} · ${orden.estado}",
                            "Agregar la pieza a la cotización de ${orden.folio}"
                        ) { agregarPiezaACotizacion(piezaId, orden) },
                        margenInferior()
                    )
                }
                estadoVista.text = if (pagina.isEmpty()) {
                    if (consulta.isEmpty()) "No hay órdenes disponibles para cotizar."
                    else "No se encontraron órdenes. Prueba otra búsqueda."
                } else ""
                estadoVista.visibility = if (pagina.isEmpty()) View.VISIBLE else View.GONE
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

    private fun agregarPiezaACotizacion(piezaId: String, orden: OrdenServicio) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val versionCotizacion = database.withTransaction {
                    val pieza = database.piezaDao().obtenerPorId(piezaId)
                        ?.takeIf { it.activo }
                        ?: throw ErrorInventario("La pieza ya no está activa.")
                    val config = database.configuracionTallerDao().obtenerPorId("TALLER")
                        ?: throw ErrorInventario("No se encontró la configuración del taller.")
                    val borrador = database.cotizacionDao()
                        .obtenerBorradorMasReciente(orden.id)
                    val cotizacion = if (borrador != null) {
                        if (borrador.monedaCodigo != config.monedaCodigo) {
                            throw ErrorInventario(
                                "La moneda del taller cambió. No se agregó la pieza al borrador."
                            )
                        }
                        borrador
                    } else {
                        val nueva = Cotizacion(
                            id = EntidadId.nuevo(),
                            ordenId = orden.id,
                            numeroVersion = Math.addExact(
                                database.cotizacionDao().obtenerMaximaVersion(orden.id) ?: 0,
                                1
                            ),
                            subtotalMinor = 0,
                            descuentoMinor = 0,
                            impuestoMinor = 0,
                            totalMinor = 0,
                            monedaCodigo = config.monedaCodigo,
                            estado = "BORRADOR",
                            creadaEn = Instant.now().toString()
                        )
                        database.cotizacionDao().insertar(nueva)
                        nueva
                    }
                    val descripcion = listOf(
                        pieza.nombre,
                        pieza.variante,
                        pieza.calidad,
                        pieza.numeroParte?.let { "Parte $it" }
                    ).filterNotNull().joinToString(" · ")
                    val detalle = DetalleCotizacion(
                        id = EntidadId.nuevo(),
                        cotizacionId = cotizacion.id,
                        tipo = "PIEZA",
                        descripcionSnapshot = descripcion,
                        cantidad = 1,
                        precioUnitarioMinor = pieza.precioVentaMinor,
                        importeMinor = pieza.precioVentaMinor,
                        piezaId = pieza.id
                    )
                    database.detalleCotizacionDao().insertar(detalle)
                    val detalles = database.detalleCotizacionDao()
                        .obtenerPorCotizacionId(cotizacion.id)
                    val totales = ReglasServicios.calcularTotales(
                        detalles = detalles,
                        descuentoMinor = cotizacion.descuentoMinor,
                        impuestoTasa = config.impuestoTasa
                    )
                    val actualizados = database.cotizacionDao().actualizarTotalesBorrador(
                        id = cotizacion.id,
                        subtotalMinor = totales.subtotalMinor,
                        impuestoMinor = totales.impuestoMinor,
                        totalMinor = totales.totalMinor
                    )
                    if (actualizados != 1) {
                        throw ErrorInventario("No se pudieron actualizar los totales del borrador.")
                    }
                    cotizacion.numeroVersion
                }
                mostrarError(
                    "Pieza agregada",
                    "Se agregó la pieza al borrador de cotización versión $versionCotizacion. " +
                        "Las existencias no cambiaron."
                ) { mostrarFicha(piezaId) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarError(
                    "No se agregó la pieza",
                    mensajeError(exception, "No se pudo actualizar el borrador.")
                ) { mostrarOrdenesParaCotizacion(piezaId, true) }
            }
        }
    }

    private fun agregarCampo(
        contenido: LinearLayout,
        hint: String,
        valor: String = "",
        tipoEntrada: Int = InputType.TYPE_CLASS_TEXT
    ): Campo {
        val layout = TextInputLayout(activity).apply {
            this.hint = hint
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            isHintEnabled = true
        }
        val input = TextInputEditText(layout.context).apply {
            inputType = tipoEntrada
            contentDescription = hint
            setText(valor)
        }
        layout.addView(input)
        contenido.addView(layout, margenInferior())
        return Campo(layout, input)
    }

    private fun crearSpinner(opciones: List<Opcion>): Spinner {
        val spinner = Spinner(activity)
        val adapter = ArrayAdapter(
            activity,
            android.R.layout.simple_spinner_item,
            opciones
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinner.adapter = adapter
        spinner.contentDescription = "Selecciona una opción"
        return spinner
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

    private fun textoFilaPieza(
        pieza: Pieza,
        saldo: SaldoInventario?,
        monedaCodigo: String
    ): String = buildString {
        append(pieza.nombre)
        append(" · ")
        append(pieza.variante)
        append(" · ")
        append(pieza.calidad)
        append("\n")
        append(pieza.codigo ?: pieza.numeroParte ?: "Sin identificador")
        append(" · ")
        append(ReglasServicios.formatearPrecio(pieza.precioVentaMinor, monedaCodigo))
        append(" · ")
        when {
            !pieza.activo -> append("Inactiva")
            saldo == null -> append("Disponible: 0")
            saldo.requiereRevision -> append("Saldo requiere revisión")
            else -> append("Disponible: ${saldo.disponible}")
        }
    }

    private fun etiquetaTipo(tipo: String): String = when (tipo) {
        TipoMovimientoInventario.ENTRADA.codigo -> "Entrada"
        TipoMovimientoInventario.CONSUMO.codigo -> "Consumo"
        TipoMovimientoInventario.RESERVA.codigo -> "Reserva"
        TipoMovimientoInventario.LIBERACION.codigo -> "Liberación"
        TipoMovimientoInventario.DEVOLUCION.codigo -> "Devolución"
        TipoMovimientoInventario.AJUSTE.codigo -> "Ajuste"
        else -> tipo
    }

    private fun mensajeError(exception: Exception, predeterminado: String): String =
        if (exception is ErrorInventario) exception.message ?: predeterminado else predeterminado

    private fun mostrarError(titulo: String, mensaje: String, reintentar: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton(if (reintentar == null) "Cerrar" else "Reintentar") { _, _ ->
                reintentar?.invoke()
            }
            .setNegativeButton(if (reintentar == null) null else "Cerrar", null)
            .show()
    }

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
        val encabezado = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val botonVolver = boton("Volver", "Regresar a la pantalla anterior") { regresar() }
        encabezado.addView(botonVolver)
        encabezado.addView(
            texto(titulo, 22f, COLOR_PRIMARIO, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        contenido.addView(encabezado, margenInferior())
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
        return contenido
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
        private val COLOR_PRIMARIO = Color.rgb(0, 117, 112)
        private val COLOR_TEXTO = Color.rgb(20, 32, 48)
        private val COLOR_SECUNDARIO = Color.rgb(91, 105, 122)
        private val COLOR_ALERTA = Color.rgb(180, 90, 0)
        private val COLOR_ERROR = Color.rgb(180, 40, 40)
    }
}
