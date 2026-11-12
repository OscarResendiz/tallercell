package com.example.tallercelulares.Modulos.Catalogos.Clientes

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
import com.example.tallercelulares.DataBase.Entidades.Cliente.Cliente
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

internal class CatalogoClientes(
    private val activity: AppCompatActivity,
    private val container: FrameLayout,
    private val database: TallerCelularesDatabase,
    private val alVolverAlTablero: () -> Unit,
    private val alBloquear: () -> Unit,
    private val alAbrirDispositivos: (String, String?) -> Unit
) {
    private data class BorradorCliente(
        val nombre: String,
        val telefono: String,
        val correo: String,
        val notas: String
    )

    private enum class Pantalla { LISTA, FICHA, FORMULARIO, CERRADA }

    private val handler = Handler(Looper.getMainLooper())
    private val busquedaPendiente = Runnable { actualizarLista(reiniciar = true) }
    private val clientes = mutableListOf<Cliente>()
    private val callbackAtras = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when (pantalla) {
                Pantalla.LISTA -> volverAlTablero()
                Pantalla.FICHA -> volverDeFicha()
                Pantalla.FORMULARIO -> cancelarFormulario()
                Pantalla.CERRADA -> Unit
            }
        }
    }

    private var pantalla = Pantalla.LISTA
    private var filtro = ""
    private var desplazamiento = 0
    private var hayMas = false
    private var cargando = false
    private var tarea: Job? = null
    private var clienteSeleccionado: String? = null
    private var editorDe: String? = null
    private var alVolverDeFicha: (() -> Unit)? = null
    private var filtroTexto: TextInputEditText? = null
    private var lista: LinearLayout? = null
    private var estado: TextView? = null
    private var botonMas: MaterialButton? = null
    private var botonReintentarLista: MaterialButton? = null

    init {
        activity.onBackPressedDispatcher.addCallback(activity, callbackAtras)
        mostrarLista()
    }

    fun refrescar() {
        when (pantalla) {
            Pantalla.LISTA -> actualizarLista(reiniciar = true)
            Pantalla.FICHA -> clienteSeleccionado?.let { mostrarFicha(it, alVolverDeFicha) }
            Pantalla.FORMULARIO, Pantalla.CERRADA -> Unit
        }
    }

    fun cerrar() {
        handler.removeCallbacksAndMessages(null)
        tarea?.cancel()
        pantalla = Pantalla.CERRADA
        callbackAtras.isEnabled = false
    }

    fun abrirFicha(id: String) {
        mostrarFicha(id)
    }

    private fun mostrarLista() {
        pantalla = Pantalla.LISTA
        editorDe = null
        val contenido = crearContenido("Clientes", mostrarAtras = true)
        contenido.addView(
            boton("Nuevo cliente", "Registrar un cliente") { mostrarFormulario() },
            margenInferior()
        )

        val campo = TextInputLayout(activity).apply {
            hint = "Buscar por nombre o teléfono"
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            contentDescription = "Buscar clientes por nombre o teléfono"
        }
        filtroTexto = TextInputEditText(campo.context).apply {
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            inputType = InputType.TYPE_CLASS_TEXT
            contentDescription = "Buscar clientes por nombre o teléfono"
            setText(filtro)
        }
        campo.addView(filtroTexto)
        contenido.addView(campo, margenInferior())

        estado = texto("Cargando clientes…", 15f, COLOR_SECUNDARIO, false)
        contenido.addView(estado)
        val reintentar = boton("Reintentar", "Volver a consultar clientes") {
            actualizarLista(reiniciar = true)
        }.apply { visibility = View.GONE }
        botonReintentarLista = reintentar
        contenido.addView(reintentar)
        lista = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        contenido.addView(lista)
        botonMas = boton("Cargar más clientes", "Cargar más clientes") {
            if (hayMas && !cargando) actualizarLista(reiniciar = false)
        }.apply { visibility = View.GONE }
        contenido.addView(botonMas)

        filtroTexto?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtro = s?.toString().orEmpty()
                handler.removeCallbacks(busquedaPendiente)
                handler.postDelayed(busquedaPendiente, DEBOUNCE_BUSQUEDA_MILLIS)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        actualizarLista(reiniciar = true)
    }

    private fun actualizarLista(reiniciar: Boolean) {
        if (pantalla != Pantalla.LISTA) return
        val estadoVista = estado ?: return
        val vistaLista = lista ?: return
        val botonSiguiente = botonMas ?: return
        val reintentar = botonReintentarLista ?: return
        if (reiniciar) {
            desplazamiento = 0
            hayMas = false
            clientes.clear()
            vistaLista.removeAllViews()
            botonSiguiente.visibility = View.GONE
            estadoVista.text = "Cargando clientes…"
            reintentar.visibility = View.GONE
        }
        tarea?.cancel()
        val consulta = ReglasClientes.patronBusqueda(filtro)
        val telefono = ReglasClientes.digitosTelefono(filtro)
        val offset = desplazamiento
        cargando = true
        tarea = activity.lifecycleScope.launch {
            try {
                val pagina = database.clienteDao().buscarActivos(
                    consulta = consulta,
                    telefono = telefono,
                    limite = PAGE_SIZE + 1,
                    desplazamiento = offset
                )
                val registros = pagina.take(PAGE_SIZE)
                hayMas = pagina.size > PAGE_SIZE
                clientes.addAll(registros)
                desplazamiento += registros.size
                registros.forEach(::agregarCliente)
                val sinFiltro = consulta.isEmpty()
                estadoVista.text = when {
                    clientes.isNotEmpty() -> ""
                    sinFiltro -> "Todavía no hay clientes activos. Puedes registrar el primero."
                    else -> "No se encontraron coincidencias. Prueba otro nombre o teléfono."
                }
                estadoVista.visibility = if (clientes.isEmpty()) View.VISIBLE else View.GONE
                reintentar.visibility = View.GONE
                botonSiguiente.visibility = if (hayMas) View.VISIBLE else View.GONE
                botonSiguiente.isEnabled = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                estadoVista.text = "No se pudieron consultar los clientes. Tus datos no se modificaron."
                estadoVista.visibility = View.VISIBLE
                reintentar.visibility = View.VISIBLE
                botonSiguiente.visibility = if (clientes.isNotEmpty()) View.VISIBLE else View.GONE
                botonSiguiente.isEnabled = true
            } finally {
                cargando = false
            }
        }
    }

    private fun agregarCliente(cliente: Cliente) {
        val fila = MaterialButton(activity).apply {
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            text = buildString {
                append(cliente.nombre)
                val contacto = cliente.telefono?.takeIf(String::isNotBlank)
                    ?: cliente.correo?.takeIf(String::isNotBlank)
                if (!contacto.isNullOrBlank()) append("\n$contacto")
            }
            contentDescription = "Abrir ficha de ${cliente.nombre}"
            setOnClickListener { mostrarFicha(cliente.id) }
        }
        lista?.addView(fila, margenInferior())
    }

    private fun mostrarFormulario(
        clienteId: String? = null,
        borrador: BorradorCliente? = null
    ) {
        pantalla = Pantalla.FORMULARIO
        editorDe = clienteId
        clienteSeleccionado = clienteId
        alVolverDeFicha = null
        tarea?.cancel()
        val contenido = crearContenido(
            if (clienteId == null) "Nuevo cliente" else "Editar cliente",
            mostrarAtras = true
        )
        val cargandoForm = texto(
            if (clienteId == null) "Preparando formulario…" else "Cargando cliente…",
            15f,
            COLOR_SECUNDARIO,
            false
        )
        contenido.addView(cargandoForm)

        tarea = activity.lifecycleScope.launch {
            val cliente = try {
                clienteId?.let { database.clienteDao().obtenerActivoPorId(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                mostrarErrorFormulario("No se pudo cargar el cliente.", clienteId)
                return@launch
            }
            if (clienteId != null && cliente == null) {
                mostrarErrorFormulario("El cliente ya no está activo o no existe.", clienteId)
                return@launch
            }
            if (pantalla != Pantalla.FORMULARIO) return@launch
            contenido.removeAllViews()
            agregarEncabezado(contenido, if (clienteId == null) "Nuevo cliente" else "Editar cliente")
            contenido.addView(
                texto("El nombre es obligatorio. Los demás datos son opcionales.", 14f, COLOR_SECUNDARIO, false),
                margenInferior()
            )
            contenido.addView(
                texto(
                    "No escribas contraseñas, PIN, patrones ni credenciales de equipos en las notas.",
                    14f,
                    COLOR_SECUNDARIO,
                    false
                ),
                margenInferior()
            )

            val layouts = mutableMapOf<String, TextInputLayout>()
            val campos = mutableMapOf<String, TextInputEditText>()
            agregarCampo(
                contenido, layouts, campos, "nombre", "Nombre",
                borrador?.nombre ?: cliente?.nombre.orEmpty()
            )
            agregarCampo(
                contenido, layouts, campos, "telefono", "Teléfono (opcional)",
                borrador?.telefono ?: cliente?.telefono.orEmpty(), InputType.TYPE_CLASS_PHONE
            )
            agregarCampo(
                contenido, layouts, campos, "correo", "Correo (opcional)",
                borrador?.correo ?: cliente?.correo.orEmpty(),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            )
            agregarCampo(
                contenido, layouts, campos, "notas", "Notas generales (opcional)",
                borrador?.notas ?: cliente?.notas.orEmpty(),
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
                multilinea = true
            )
            val mensaje = texto("", 14f, COLOR_ERROR, false)
            contenido.addView(mensaje, margenInferior())
            val guardar = boton("Guardar cliente", "Guardar datos del cliente") {}
            guardar.setOnClickListener {
                val nombre = campos["nombre"]?.text?.toString().orEmpty()
                val telefono = campos["telefono"]?.text?.toString().orEmpty()
                val correo = campos["correo"]?.text?.toString().orEmpty()
                val notas = campos["notas"]?.text?.toString().orEmpty()
                layouts.values.forEach { it.error = null }
                val errores = ReglasClientes.validar(nombre, telefono, correo, notas)
                errores.forEach { (campo, error) -> layouts[campo]?.error = error }
                if (errores.isEmpty()) {
                    guardar.isEnabled = false
                    mensaje.text = "Guardando…"
                    if (clienteId == null) {
                        revisarCoincidenciasYGuardar(
                            nombre, telefono, correo, notas, guardar, mensaje
                        )
                    } else {
                        guardarEdicion(
                            clienteId, nombre, telefono, correo, notas, guardar, mensaje
                        )
                    }
                }
            }
            contenido.addView(guardar, margenInferior())
        }
    }

    private fun revisarCoincidenciasYGuardar(
        nombre: String,
        telefono: String,
        correo: String,
        notas: String,
        botonGuardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            try {
                val coincidencias = database.clienteDao().buscarCoincidenciasActivas(
                    nombre = ReglasClientes.patronBusqueda(nombre),
                    telefono = ReglasClientes.digitosTelefono(telefono),
                    limite = LIMITE_COINCIDENCIAS
                )
                if (pantalla != Pantalla.FORMULARIO) return@launch
                if (coincidencias.isEmpty()) {
                    crearCliente(nombre, telefono, correo, notas, botonGuardar, mensaje)
                } else {
                    val borrador = BorradorCliente(nombre, telefono, correo, notas)
                    val opciones = coincidencias.map { cliente ->
                        listOfNotNull(cliente.nombre, cliente.telefono, cliente.correo)
                            .joinToString(" · ")
                    }.toTypedArray()
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Posibles coincidencias; un teléfono compartido no impide crear")
                        .setItems(opciones) { _, posicion ->
                            val coincidencia = coincidencias[posicion]
                            mostrarFicha(coincidencia.id) {
                                mostrarFormulario(borrador = borrador)
                            }
                        }
                        .setPositiveButton("Crear de todos modos") { _, _ ->
                            crearCliente(nombre, telefono, correo, notas, botonGuardar, mensaje)
                        }
                        .setNegativeButton("Seguir revisando") { _, _ ->
                            botonGuardar.isEnabled = true
                            mensaje.text = ""
                        }
                        .setOnCancelListener {
                            botonGuardar.isEnabled = true
                            mensaje.text = ""
                        }
                        .show()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                botonGuardar.isEnabled = true
                mensaje.text = "No se pudieron revisar coincidencias. Intenta guardar de nuevo."
            }
        }
    }

    private fun crearCliente(
        nombre: String,
        telefono: String,
        correo: String,
        notas: String,
        botonGuardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            val ahora = Instant.now().toString()
            val nuevo = Cliente(
                id = EntidadId.nuevo(),
                nombre = nombre.trim(),
                telefono = ReglasClientes.textoOpcional(telefono),
                correo = ReglasClientes.textoOpcional(correo),
                notas = ReglasClientes.textoOpcional(notas),
                creadoEn = ahora,
                actualizadoEn = ahora
            )
            try {
                database.clienteDao().insertar(nuevo)
                val guardado = database.clienteDao().obtenerActivoPorId(nuevo.id)
                    ?: error("No se pudo volver a consultar el cliente guardado.")
                mostrarFicha(guardado.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                botonGuardar.isEnabled = true
                mensaje.text = "No se pudo guardar el cliente. Revisa los datos e inténtalo de nuevo."
            }
        }
    }

    private fun guardarEdicion(
        id: String,
        nombre: String,
        telefono: String,
        correo: String,
        notas: String,
        botonGuardar: MaterialButton,
        mensaje: TextView
    ) {
        activity.lifecycleScope.launch {
            try {
                val actualizados = database.clienteDao().actualizarDatos(
                    id = id,
                    nombre = nombre.trim(),
                    telefono = ReglasClientes.textoOpcional(telefono),
                    correo = ReglasClientes.textoOpcional(correo),
                    notas = ReglasClientes.textoOpcional(notas),
                    actualizadoEn = Instant.now().toString()
                )
                if (actualizados != 1) error("El cliente dejó de estar activo.")
                val guardado = database.clienteDao().obtenerActivoPorId(id)
                    ?: error("No se pudo volver a consultar el cliente actualizado.")
                mostrarFicha(guardado.id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                botonGuardar.isEnabled = true
                mensaje.text = "No se pudo actualizar el cliente. Tus cambios siguen en el formulario."
            }
        }
    }

    private fun mostrarErrorFormulario(mensaje: String, clienteId: String?) {
        if (pantalla != Pantalla.FORMULARIO) return
        val contenido = crearContenido(
            if (clienteId == null) "Nuevo cliente" else "Editar cliente",
            mostrarAtras = true
        )
        contenido.addView(texto(mensaje, 15f, COLOR_ERROR, false), margenInferior())
        contenido.addView(boton("Reintentar", "Volver a cargar el formulario") {
            mostrarFormulario(clienteId)
        })
    }

    private fun cancelarFormulario() {
        val id = editorDe
        if (id == null) mostrarLista() else mostrarFicha(id)
    }

    private fun volverDeFicha() {
        val accion = alVolverDeFicha
        alVolverDeFicha = null
        if (accion == null) mostrarLista() else accion()
    }

    private fun mostrarFicha(id: String, alVolver: (() -> Unit)? = null) {
        pantalla = Pantalla.FICHA
        clienteSeleccionado = id
        alVolverDeFicha = alVolver
        tarea?.cancel()
        val contenido = crearContenido("Ficha del cliente", mostrarAtras = true)
        contenido.addView(texto("Cargando información del cliente…", 15f, COLOR_SECUNDARIO, false))
        tarea = activity.lifecycleScope.launch {
            try {
                val cliente = database.clienteDao().obtenerPorId(id)
                    ?: error("No se encontró el cliente.")
                val dispositivos = database.dispositivoDao().obtenerPorClienteId(id)
                val ordenes = database.ordenServicioDao().obtenerPorClienteId(id)
                if (pantalla != Pantalla.FICHA || clienteSeleccionado != id) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del cliente")
                if (cliente.eliminadoEn != null) {
                    contenido.addView(
                        texto("Cliente inactivo desde ${cliente.eliminadoEn}", 15f, COLOR_ERROR, true),
                        margenInferior()
                    )
                }
                contenido.addView(texto(cliente.nombre, 22f, COLOR_TEXTO, true))
                contenido.addView(
                    texto("Teléfono: ${cliente.telefono ?: "No registrado"}", 16f, COLOR_TEXTO, false)
                )
                contenido.addView(
                    texto("Correo: ${cliente.correo ?: "No registrado"}", 16f, COLOR_TEXTO, false)
                )
                contenido.addView(texto("Notas generales", 17f, COLOR_TEXTO, true), margenSuperior())
                contenido.addView(
                    texto(
                        cliente.notas ?: "Sin notas.",
                        15f,
                        COLOR_SECUNDARIO,
                        false
                    )
                )
                if (cliente.eliminadoEn == null) {
                    contenido.addView(
                        boton("Editar cliente", "Editar datos de ${cliente.nombre}") {
                            mostrarFormulario(id)
                        },
                        margenSuperior()
                    )
                    contenido.addView(
                        boton("Dar de baja", "Dar de baja a ${cliente.nombre}") {
                            confirmarBaja(cliente, dispositivos.size, ordenes.size)
                        },
                        margenInferior()
                    )
                }

                contenido.addView(texto("Dispositivos", 19f, COLOR_TEXTO, true), margenSuperior())
                contenido.addView(
                    boton(
                        if (cliente.eliminadoEn == null) "Administrar dispositivos" else "Consultar dispositivos",
                        "Abrir dispositivos de ${cliente.nombre}"
                    ) { alAbrirDispositivos(id, null) },
                    margenInferior()
                )
                if (dispositivos.isEmpty()) {
                    contenido.addView(texto("Sin dispositivos registrados.", 15f, COLOR_SECUNDARIO, false))
                } else {
                    dispositivos.forEach { dispositivo ->
                        contenido.addView(
                            boton(
                                listOf(dispositivo.tipo, dispositivo.marca, dispositivo.modelo, dispositivo.color)
                                    .filter(String::isNotBlank)
                                    .joinToString(" · ") +
                                    if (dispositivo.eliminadoEn != null) "\nInactivo" else "",
                                "Abrir ficha del dispositivo"
                            ) { alAbrirDispositivos(id, dispositivo.id) },
                            margenInferior()
                        )
                    }
                }

                contenido.addView(texto("Historial de órdenes", 19f, COLOR_TEXTO, true), margenSuperior())
                if (ordenes.isEmpty()) {
                    contenido.addView(texto("Sin órdenes relacionadas.", 15f, COLOR_SECUNDARIO, false))
                } else {
                    ordenes.forEach { orden ->
                        val detalle = buildString {
                            append("${orden.folio} · ${orden.estado}")
                            append("\n${orden.tipoEquipoSnapshot} ${orden.marcaSnapshot} ${orden.modeloSnapshot}")
                            append("\nRecibida: ${orden.recibidaEn}")
                            if (orden.eliminadaEn != null) append("\nOrden archivada")
                        }
                        contenido.addView(texto(detalle, 15f, COLOR_TEXTO, false), margenInferior())
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                if (pantalla != Pantalla.FICHA || clienteSeleccionado != id) return@launch
                contenido.removeAllViews()
                agregarEncabezado(contenido, "Ficha del cliente")
                contenido.addView(
                    texto(
                        "No se pudo consultar la ficha y su historial. Intenta de nuevo.",
                        15f,
                        COLOR_ERROR,
                        false
                    ),
                    margenInferior()
                )
                contenido.addView(boton("Reintentar", "Volver a consultar ficha") {
                    mostrarFicha(id)
                })
            }
        }
    }

    private fun confirmarBaja(cliente: Cliente, cantidadDispositivos: Int, cantidadOrdenes: Int) {
        val impacto = "El cliente dejará de aparecer en el catálogo activo. " +
            "Sus datos, dispositivos y órdenes se conservarán."
        val relaciones = "Se conservarán $cantidadDispositivos dispositivos y $cantidadOrdenes órdenes relacionadas."
        MaterialAlertDialogBuilder(activity)
            .setTitle("Dar de baja a ${cliente.nombre}?")
            .setMessage("$impacto\n\n$relaciones")
            .setPositiveButton("Confirmar baja") { _, _ -> realizarBaja(cliente.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun realizarBaja(id: String) {
        tarea?.cancel()
        tarea = activity.lifecycleScope.launch {
            try {
                val ahora = Instant.now().toString()
                val actualizados = database.clienteDao().darDeBaja(id, ahora, ahora)
                if (actualizados != 1) error("El cliente ya no está activo.")
                mostrarLista()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("No se pudo dar de baja")
                    .setMessage("El cliente se conserva sin cambios. Intenta de nuevo.")
                    .setPositiveButton("Reintentar") { _, _ -> realizarBaja(id) }
                    .setNegativeButton("Cerrar", null)
                    .show()
            }
        }
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
            texto(titulo, 24f, COLOR_TEXTO, true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        if (mostrarAtras) {
            encabezado.addView(boton("Atrás", "Volver") {
                when (pantalla) {
                    Pantalla.LISTA -> volverAlTablero()
                    Pantalla.FICHA -> volverDeFicha()
                    Pantalla.FORMULARIO -> cancelarFormulario()
                    Pantalla.CERRADA -> Unit
                }
            })
        }
        encabezado.addView(boton("Bloquear", "Bloquear la aplicación") { cerrarYBloquear() })
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
            isHintEnabled = true
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

    private fun boton(
        titulo: String,
        descripcion: String,
        accion: () -> Unit
    ): MaterialButton = MaterialButton(activity).apply {
        text = titulo
        isAllCaps = false
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        contentDescription = descripcion
        setOnClickListener { accion() }
    }

    private fun texto(valor: String, tamano: Float, color: Int, negrita: Boolean): TextView =
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
        ).apply { topMargin = dp(16) }

    private fun dp(valor: Int): Int =
        (valor * activity.resources.displayMetrics.density).toInt()

    private fun volverAlTablero() {
        handler.removeCallbacksAndMessages(null)
        tarea?.cancel()
        pantalla = Pantalla.CERRADA
        callbackAtras.isEnabled = false
        alVolverAlTablero()
    }

    private fun cerrarYBloquear() {
        handler.removeCallbacksAndMessages(null)
        tarea?.cancel()
        pantalla = Pantalla.CERRADA
        callbackAtras.isEnabled = false
        alBloquear()
    }

    companion object {
        private const val PAGE_SIZE = 50
        private const val LIMITE_COINCIDENCIAS = 5
        private const val DEBOUNCE_BUSQUEDA_MILLIS = 300L
        private val COLOR_TEXTO = Color.rgb(20, 32, 48)
        private val COLOR_SECUNDARIO = Color.rgb(91, 105, 122)
        private val COLOR_ERROR = Color.rgb(151, 55, 20)
    }
}
