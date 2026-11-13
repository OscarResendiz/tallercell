package com.example.tallercelulares.Modulos.Catalogos.Piezas

import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventario
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Currency

internal enum class TipoMovimientoInventario(val codigo: String) {
    ENTRADA("ENTRADA"),
    CONSUMO("CONSUMO"),
    RESERVA("RESERVA"),
    LIBERACION("LIBERACION"),
    DEVOLUCION("DEVOLUCION"),
    AJUSTE("AJUSTE");

    companion object {
        fun desdeCodigo(codigo: String): TipoMovimientoInventario? =
            entries.firstOrNull { it.codigo == codigo }
    }
}

internal enum class SentidoAjuste(val codigo: String) {
    INCREMENTO("INCREMENTO"),
    DECREMENTO("DECREMENTO")
}

internal data class SaldoInventario(
    val existenciaFisica: Long,
    val cantidadReservada: Long,
    val disponible: Long,
    val requiereRevision: Boolean = false
)

internal data class CamposPieza(
    val codigo: String,
    val numeroParte: String,
    val nombre: String,
    val variante: String,
    val calidad: String,
    val costo: String,
    val precioVenta: String,
    val existenciaMinima: String
)

internal object ReglasPiezas {
    private const val MAX_IDENTIFICADOR = 80
    private const val MAX_NOMBRE = 120
    private const val MAX_VARIANTE = 120
    private const val MAX_CALIDAD = 80
    private const val MAX_MOTIVO = 500
    private val patronDecimal = Regex("""(?:\d+(?:[.,]\d*)?|[.,]\d+)""")

    fun validarPieza(campos: CamposPieza, monedaCodigo: String): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        validarOpcional("codigo", campos.codigo, MAX_IDENTIFICADOR, errores)
        validarOpcional("numeroParte", campos.numeroParte, MAX_IDENTIFICADOR, errores)
        validarRequerido("nombre", campos.nombre, MAX_NOMBRE, errores)
        validarRequerido("variante", campos.variante, MAX_VARIANTE, errores)
        validarRequerido("calidad", campos.calidad, MAX_CALIDAD, errores)
        if (precioMinor(campos.costo, monedaCodigo) == null) {
            errores["costo"] = "Escribe un costo válido para la moneda del taller."
        }
        if (precioMinor(campos.precioVenta, monedaCodigo) == null) {
            errores["precioVenta"] = "Escribe un precio válido para la moneda del taller."
        }
        val minimo = campos.existenciaMinima.trim().toIntOrNull()
        if (minimo == null || minimo < 0) {
            errores["existenciaMinima"] = "Escribe una cantidad entera no negativa."
        }
        return errores
    }

    fun normalizarOpcional(valor: String): String? =
        valor.trim().takeIf(String::isNotEmpty)

    fun normalizarBusqueda(consulta: String): String = consulta.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    fun precioMinor(texto: String, monedaCodigo: String): Long? {
        val valor = texto.trim()
        if (!patronDecimal.matches(valor)) return null
        val moneda = try {
            Currency.getInstance(monedaCodigo)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val digitos = moneda.defaultFractionDigits
        if (digitos < 0) return null
        val importe = valor.replace(',', '.').toBigDecimalOrNull() ?: return null
        if (importe.signum() < 0) return null
        return try {
            importe.setScale(digitos, RoundingMode.UNNECESSARY)
                .movePointRight(digitos)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun textoPrecio(minor: Long, monedaCodigo: String): String {
        val digitos = Currency.getInstance(monedaCodigo).defaultFractionDigits
        require(digitos >= 0) { "La moneda no admite conversión a unidades menores." }
        return BigDecimal.valueOf(minor, digitos).toPlainString()
    }

    fun calcularSaldos(movimientos: List<MovimientoInventario>): SaldoInventario {
        val porId = movimientos.associateBy { it.id }
        val confirmados = movimientos.filter { it.confirmadaEn != null }
        val remanentesPorReserva = confirmados
            .filter { it.tipo == TipoMovimientoInventario.RESERVA.codigo }
            .associate { it.id to it.cantidad.toLong() }
            .toMutableMap()
        val devueltoPorConsumo = mutableMapOf<String, Long>()
        var existencia = 0L
        var reservada = 0L

        confirmados.sortedWith(
            compareBy<MovimientoInventario> { Instant.parse(it.confirmadaEn) }
                .thenBy { it.id }
        ).forEach { movimiento ->
            require(movimiento.cantidad > 0) {
                "Un movimiento confirmado tiene una cantidad inválida."
            }
            val cantidad = movimiento.cantidad.toLong()
            when (TipoMovimientoInventario.desdeCodigo(movimiento.tipo)) {
                TipoMovimientoInventario.ENTRADA,
                TipoMovimientoInventario.DEVOLUCION -> {
                    if (movimiento.tipo == TipoMovimientoInventario.DEVOLUCION.codigo) {
                        val origen = movimiento.movimientoOrigenId?.let(porId::get)
                        require(
                            origen?.tipo == TipoMovimientoInventario.CONSUMO.codigo &&
                                origen.piezaId == movimiento.piezaId &&
                                origen.confirmadaEn != null
                        ) { "La devolución no referencia un consumo confirmado de la misma pieza." }
                        val devuelto = Math.addExact(
                            devueltoPorConsumo[origen.id] ?: 0L,
                            cantidad
                        )
                        require(devuelto <= origen.cantidad.toLong()) {
                            "Las devoluciones exceden la cantidad del consumo original."
                        }
                        devueltoPorConsumo[origen.id] = devuelto
                    }
                    existencia = Math.addExact(existencia, cantidad)
                }

                TipoMovimientoInventario.CONSUMO -> {
                    val origen = movimiento.movimientoOrigenId?.let(porId::get)
                    if (movimiento.movimientoOrigenId != null) {
                        require(
                            origen?.tipo == TipoMovimientoInventario.RESERVA.codigo &&
                                origen.piezaId == movimiento.piezaId &&
                                origen.confirmadaEn != null
                        ) { "El consumo no referencia una reserva confirmada de la misma pieza." }
                    }
                    if (origen != null) {
                        reservar(origen, cantidad, remanentesPorReserva)
                        reservada = Math.subtractExact(reservada, cantidad)
                    } else {
                        require(cantidad <= Math.subtractExact(existencia, reservada)) {
                            "El consumo excede la existencia disponible en ese momento."
                        }
                    }
                    existencia = Math.subtractExact(existencia, cantidad)
                }

                TipoMovimientoInventario.RESERVA -> {
                    if (movimiento.movimientoOrigenId != null) {
                        val origen = porId[movimiento.movimientoOrigenId]
                        require(
                            origen != null &&
                                origen.piezaId == movimiento.piezaId &&
                                origen.confirmadaEn != null &&
                                origen.tipo in setOf(
                                    TipoMovimientoInventario.CONSUMO.codigo,
                                    TipoMovimientoInventario.LIBERACION.codigo
                                )
                        ) { "La reserva de compensación no referencia una operación válida." }
                    }
                    require(cantidad <= Math.subtractExact(existencia, reservada)) {
                        "La reserva excede la existencia disponible en ese momento."
                    }
                    reservada = Math.addExact(reservada, cantidad)
                }

                TipoMovimientoInventario.LIBERACION -> {
                    require(cantidad <= reservada) {
                        "La liberación excede las piezas reservadas en ese momento."
                    }
                    reservada = Math.subtractExact(reservada, cantidad)
                    val origen = movimiento.movimientoOrigenId?.let(porId::get)
                    if (origen != null) {
                        require(
                            origen.tipo == TipoMovimientoInventario.RESERVA.codigo &&
                                origen.piezaId == movimiento.piezaId &&
                                origen.confirmadaEn != null
                        ) {
                            "La liberación no referencia una reserva confirmada de la misma pieza."
                        }
                        reservar(origen, cantidad, remanentesPorReserva)
                    }
                }

                TipoMovimientoInventario.AJUSTE -> {
                    existencia = when (movimiento.sentidoAjuste) {
                        SentidoAjuste.INCREMENTO.codigo -> Math.addExact(existencia, cantidad)
                        SentidoAjuste.DECREMENTO.codigo -> {
                            require(cantidad <= Math.subtractExact(existencia, reservada)) {
                                "El ajuste deja menos existencia que la cantidad reservada."
                            }
                            Math.subtractExact(existencia, cantidad)
                        }
                        else -> throw IllegalStateException(
                            "Hay un ajuste histórico sin sentido. No se puede calcular el inventario con seguridad."
                        )
                    }
                }

                null -> throw IllegalStateException(
                    "Hay un movimiento con tipo desconocido. No se puede calcular el inventario con seguridad."
                )
            }
            require(existencia >= 0 && reservada >= 0 && reservada <= existencia) {
                "Un movimiento confirmado produce un saldo físico o reservado inválido."
            }
        }

        return SaldoInventario(
            existenciaFisica = existencia,
            cantidadReservada = reservada,
            disponible = Math.subtractExact(existencia, reservada)
        )
    }

    fun remanenteReserva(
        reservaId: String,
        movimientos: List<MovimientoInventario>
    ): Long {
        val reserva = movimientos.firstOrNull { it.id == reservaId }
            ?.takeIf {
                it.tipo == TipoMovimientoInventario.RESERVA.codigo && it.confirmadaEn != null
            } ?: throw IllegalArgumentException("No se encontró la reserva confirmada.")
        val usados = movimientos.asSequence()
            .filter {
                it.confirmadaEn != null &&
                    it.movimientoOrigenId == reservaId &&
                    (it.tipo == TipoMovimientoInventario.LIBERACION.codigo ||
                        it.tipo == TipoMovimientoInventario.CONSUMO.codigo)
            }
            .fold(0L) { total, movimiento ->
                Math.addExact(total, movimiento.cantidad.toLong())
            }
        return Math.subtractExact(reserva.cantidad.toLong(), usados)
    }

    private fun reservar(
        origen: MovimientoInventario,
        cantidad: Long,
        remanentes: MutableMap<String, Long>
    ) {
        require(origen.tipo == TipoMovimientoInventario.RESERVA.codigo) {
            "El movimiento de origen no es una reserva."
        }
        val remanente = remanentes[origen.id]
            ?: origen.cantidad.toLong().also { remanentes[origen.id] = it }
        require(remanente >= cantidad) {
            "La operación excede el remanente de la reserva."
        }
        remanentes[origen.id] = Math.subtractExact(remanente, cantidad)
    }

    private fun validarRequerido(
        campo: String,
        valor: String,
        maximo: Int,
        errores: MutableMap<String, String>
    ) {
        when {
            valor.trim().isEmpty() -> errores[campo] = "Este campo es obligatorio."
            valor.trim().length > maximo -> errores[campo] = "Máximo $maximo caracteres."
        }
    }

    private fun validarOpcional(
        campo: String,
        valor: String,
        maximo: Int,
        errores: MutableMap<String, String>
    ) {
        if (valor.trim().length > maximo) errores[campo] = "Máximo $maximo caracteres."
    }
}
