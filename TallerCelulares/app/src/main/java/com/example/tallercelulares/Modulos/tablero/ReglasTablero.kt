package com.example.tallercelulares.Modulos.tablero

import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenTableroRegistro
import java.text.Normalizer
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class GrupoTablero(val etiqueta: String, val estados: List<String>) {
    DIAGNOSTICO(
        "Diagnóstico",
        listOf("RECIBIDA", "COLA_DIAGNOSTICO", "EN_DIAGNOSTICO")
    ),
    ESPERA(
        "En espera",
        listOf("ESPERANDO_AUTORIZACION", "ESPERANDO_REFACCION")
    ),
    REPARACION(
        "Reparación",
        listOf("COLA_REPARACION", "EN_REPARACION", "CONTROL_CALIDAD")
    ),
    ENTREGA(
        "Entrega",
        listOf("LISTA_ENTREGA", "DEVOLUCION_SIN_REPARAR")
    )
}

internal data class AlertaTablero(
    val titulo: String,
    val detalle: String
)

internal data class TarjetaOrdenTablero(
    val orden: OrdenTableroRegistro,
    val estado: String,
    val antiguedad: String,
    val tiempoEnEstado: String,
    val proximaAccion: String,
    val alerta: AlertaTablero?
)

internal object ReglasTablero {
    private val terminales = setOf("ENTREGADA", "CANCELADA")
    private val fechaFormato =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX"))

    fun etiquetaEstado(codigo: String): String = when (codigo) {
        "RECIBIDA" -> "Recibida"
        "COLA_DIAGNOSTICO" -> "En cola de diagnóstico"
        "EN_DIAGNOSTICO" -> "En diagnóstico"
        "ESPERANDO_AUTORIZACION" -> "Esperando autorización"
        "ESPERANDO_REFACCION" -> "Esperando refacción"
        "COLA_REPARACION" -> "En cola de reparación"
        "EN_REPARACION" -> "En reparación"
        "CONTROL_CALIDAD" -> "En control de calidad"
        "LISTA_ENTREGA" -> "Lista para entrega"
        "DEVOLUCION_SIN_REPARAR" -> "Devolución sin reparar"
        "ENTREGADA" -> "Entregada"
        "CANCELADA" -> "Cancelada"
        else -> "Estado: $codigo"
    }

    fun proximaAccion(codigo: String): String = when (codigo) {
        "RECIBIDA", "COLA_DIAGNOSTICO", "EN_DIAGNOSTICO" -> "Completar o revisar el diagnóstico"
        "ESPERANDO_AUTORIZACION" -> "Dar seguimiento a la decisión del cliente"
        "ESPERANDO_REFACCION" -> "Revisar disponibilidad de la refacción"
        "COLA_REPARACION", "EN_REPARACION" -> "Revisar el avance de la reparación"
        "CONTROL_CALIDAD" -> "Completar las pruebas de calidad"
        "LISTA_ENTREGA" -> "Coordinar la entrega del equipo"
        "DEVOLUCION_SIN_REPARAR" -> "Revisar la devolución pendiente"
        "ENTREGADA", "CANCELADA" -> "Sin acción operativa pendiente"
        else -> "Revisar orden"
    }

    fun grupo(codigo: String): GrupoTablero? = GrupoTablero.entries
        .firstOrNull { codigo in it.estados }

    fun esTerminal(codigo: String): Boolean = codigo in terminales

    fun normalizarBusqueda(valor: String): String = Normalizer
        .normalize(valor, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")

    fun coincideBusqueda(orden: OrdenTableroRegistro, consulta: String): Boolean {
        val texto = normalizarBusqueda(consulta)
        if (texto.isEmpty()) return true
        val camposTexto = listOfNotNull(
            orden.folio,
            orden.cliente,
            orden.tipoEquipo,
            orden.marca,
            orden.modelo,
            orden.serie,
            orden.imei
        )
        if (camposTexto.any { normalizarBusqueda(it).contains(texto) }) return true

        val digitosConsulta = consulta.filter(Char::isDigit)
        val digitosTelefono = orden.contacto.orEmpty().filter(Char::isDigit)
        return digitosConsulta.length >= 3 && digitosTelefono.contains(digitosConsulta)
    }

    fun crearTarjeta(
        orden: OrdenTableroRegistro,
        ahora: Instant,
        zonaHoraria: ZoneId,
        diasSeguimientoAutorizacion: Int,
        diasRefaccionAtrasada: Int,
        diasEquipoListo: Int
    ): TarjetaOrdenTablero {
        val recibida = parsearInstante(orden.recibidaEn)
        val estadoDesde = parsearInstante(orden.estadoDesde) ?: recibida
        val fechaPrometida = orden.fechaPrometida?.let(::parsearInstante)
        val diasEstancamiento = when (orden.estado) {
            "ESPERANDO_AUTORIZACION" -> diasSeguimientoAutorizacion
            "ESPERANDO_REFACCION" -> diasRefaccionAtrasada
            "LISTA_ENTREGA" -> diasEquipoListo
            else -> 0
        }
        val diasEnEstado = estadoDesde?.let { Duration.between(it, ahora).toDays().coerceAtLeast(0) }
        val alerta = when {
            !esTerminal(orden.estado) && fechaPrometida != null && fechaPrometida.isBefore(ahora) ->
                AlertaTablero(
                    "Promesa vencida",
                    "La fecha prometida fue ${formatear(fechaPrometida, zonaHoraria)}."
                )
            !esTerminal(orden.estado) && diasEstancamiento > 0 &&
                diasEnEstado != null && diasEnEstado >= diasEstancamiento ->
                AlertaTablero(
                    "Seguimiento recomendado",
                    "Lleva $diasEnEstado días en este estado (plazo configurado: $diasEstancamiento días)."
                )
            else -> null
        }

        return TarjetaOrdenTablero(
            orden = orden,
            estado = etiquetaEstado(orden.estado),
            antiguedad = recibida?.let { duracionLegible(it, ahora) } ?: "No disponible",
            tiempoEnEstado = estadoDesde?.let { duracionLegible(it, ahora) } ?: "No disponible",
            proximaAccion = proximaAccion(orden.estado),
            alerta = alerta
        )
    }

    private fun parsearInstante(valor: String): Instant? =
        runCatching { Instant.parse(valor) }.getOrNull()

    private fun formatear(instante: Instant, zonaHoraria: ZoneId): String =
        fechaFormato.withZone(zonaHoraria).format(instante)

    private fun duracionLegible(desde: Instant, hasta: Instant): String {
        val duracion = Duration.between(desde, hasta)
        if (duracion.isNegative) return "Menos de una hora"
        val dias = duracion.toDays()
        if (dias > 0) return "$dias ${if (dias == 1L) "día" else "días"}"
        val horas = duracion.toHours()
        if (horas > 0) return "$horas ${if (horas == 1L) "hora" else "horas"}"
        return "Menos de una hora"
    }
}
