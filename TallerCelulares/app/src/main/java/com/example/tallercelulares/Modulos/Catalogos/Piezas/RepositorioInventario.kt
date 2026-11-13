package com.example.tallercelulares.Modulos.Catalogos.Piezas

import androidx.room.withTransaction
import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventario
import com.example.tallercelulares.DataBase.Entidades.Pieza.Pieza
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import java.time.DateTimeException
import java.time.Instant

internal class ErrorInventario(message: String) : Exception(message)

internal data class SolicitudMovimiento(
    val piezaId: String,
    val tipo: TipoMovimientoInventario,
    val cantidad: Int,
    val motivo: String?,
    val ordenId: String? = null,
    val costoUnitarioMinor: Long? = null,
    val sentidoAjuste: SentidoAjuste? = null,
    val movimientoOrigenId: String? = null
)

internal class RepositorioInventario(private val database: TallerCelularesDatabase) {
    suspend fun registrar(solicitud: SolicitudMovimiento): SaldoInventario =
        database.withTransaction {
            val pieza = obtenerPieza(solicitud.piezaId)
            if (!pieza.activo) throw ErrorInventario("La pieza está inactiva.")
            val movimientos = database.movimientoInventarioDao()
                .obtenerPorPiezaId(pieza.id)
            val saldo = calcularSaldo(movimientos)
            val motivo = validarSolicitud(solicitud)
            val origen = resolverOrigen(solicitud, movimientos)
            validarDisponibilidad(solicitud, saldo, origen, movimientos)
            validarOrden(solicitud.ordenId, origen)

            val ahora = Instant.now().toString()
            val movimiento = MovimientoInventario(
                id = EntidadId.nuevo(),
                piezaId = pieza.id,
                ordenId = solicitud.ordenId ?: origen?.ordenId,
                tipo = solicitud.tipo.codigo,
                cantidad = solicitud.cantidad,
                costoUnitarioMinor = solicitud.costoUnitarioMinor,
                confirmadaEn = ahora,
                motivo = motivo,
                creadoEn = ahora,
                sentidoAjuste = solicitud.sentidoAjuste?.codigo,
                movimientoOrigenId = solicitud.movimientoOrigenId
            )
            database.movimientoInventarioDao().insertar(movimiento)
            calcularSaldo(movimientos + movimiento)
        }

    suspend fun compensar(movimientoId: String, motivo: String): SaldoInventario =
        database.withTransaction {
            val dao = database.movimientoInventarioDao()
            val original = dao.obtenerPorId(movimientoId)
                ?.takeIf { it.confirmadaEn != null }
                ?: throw ErrorInventario("No se encontró el movimiento confirmado.")
            val movimientos = dao.obtenerPorPiezaId(original.piezaId)
            if (dao.obtenerDerivados(original.id).isNotEmpty()) {
                throw ErrorInventario(
                    "El movimiento ya tiene operaciones relacionadas y no puede compensarse por completo."
                )
            }
            val nota = motivo.trim()
            if (nota.isEmpty() || nota.length > Reglas.MAX_MOTIVO) {
                throw ErrorInventario("Escribe un motivo de hasta ${Reglas.MAX_MOTIVO} caracteres.")
            }
            val tipoOriginal = TipoMovimientoInventario.desdeCodigo(original.tipo)
                ?: throw ErrorInventario("El tipo de movimiento original no es compatible.")
            calcularSaldo(movimientos)
            val ahora = Instant.now().toString()
            val compensatorios = when (tipoOriginal) {
                TipoMovimientoInventario.ENTRADA ->
                    listOf(compensatorio(original, TipoMovimientoInventario.AJUSTE, ahora, nota,
                        sentido = SentidoAjuste.DECREMENTO))

                TipoMovimientoInventario.CONSUMO -> {
                    val devolucion = compensatorio(
                        original, TipoMovimientoInventario.DEVOLUCION, ahora, nota
                    )
                    val reservaOrigen = original.movimientoOrigenId
                        ?.let { id -> movimientos.firstOrNull { it.id == id } }
                    if (reservaOrigen != null) {
                        val instanteReserva = Instant.parse(ahora).plusNanos(1).toString()
                        listOf(
                            devolucion,
                            MovimientoInventario(
                                id = EntidadId.nuevo(),
                                piezaId = original.piezaId,
                                ordenId = original.ordenId,
                                tipo = TipoMovimientoInventario.RESERVA.codigo,
                                cantidad = original.cantidad,
                                confirmadaEn = instanteReserva,
                                motivo = nota,
                                creadoEn = instanteReserva,
                                movimientoOrigenId = original.id
                            )
                        )
                    } else {
                        listOf(devolucion)
                    }
                }

                TipoMovimientoInventario.RESERVA -> {
                    if (ReglasPiezas.remanenteReserva(original.id, movimientos) != original.cantidad.toLong()) {
                        throw ErrorInventario(
                            "La reserva ya se utilizó parcialmente; corrige sus operaciones pendientes por separado."
                        )
                    }
                    listOf(compensatorio(
                        original, TipoMovimientoInventario.LIBERACION, ahora, nota
                    ))
                }

                TipoMovimientoInventario.LIBERACION ->
                    listOf(compensatorio(original, TipoMovimientoInventario.RESERVA, ahora, nota))

                TipoMovimientoInventario.DEVOLUCION ->
                    listOf(compensatorio(original, TipoMovimientoInventario.AJUSTE, ahora, nota,
                        sentido = SentidoAjuste.DECREMENTO))

                TipoMovimientoInventario.AJUSTE -> listOf(
                    compensatorio(
                        original,
                        TipoMovimientoInventario.AJUSTE,
                        ahora,
                        nota,
                        sentido = when (original.sentidoAjuste) {
                            SentidoAjuste.INCREMENTO.codigo -> SentidoAjuste.DECREMENTO
                            SentidoAjuste.DECREMENTO.codigo -> SentidoAjuste.INCREMENTO
                            else -> throw ErrorInventario(
                                "El ajuste original no tiene sentido registrado y no se puede compensar."
                            )
                        }
                    )
                )
            }

            validarCompensatorios(compensatorios, movimientos)
            for (compensatorio in compensatorios) {
                dao.insertar(compensatorio)
            }
            calcularSaldo(movimientos + compensatorios)
        }

    suspend fun saldo(piezaId: String): SaldoInventario =
        calcularSaldo(database.movimientoInventarioDao().obtenerPorPiezaId(piezaId))

    suspend fun saldos(piezaIds: List<String>): Map<String, SaldoInventario> {
        if (piezaIds.isEmpty()) return emptyMap()
        val movimientosPorPieza = database.movimientoInventarioDao()
            .obtenerPorPiezaIds(piezaIds.distinct())
            .groupBy(MovimientoInventario::piezaId)
        return piezaIds.distinct().associateWith { piezaId ->
            val movimientos = movimientosPorPieza[piezaId].orEmpty()
            if (movimientos.isEmpty()) {
                SaldoInventario(0, 0, 0)
            } else {
                calcularSaldoParaConsulta(movimientos)
            }
        }
    }

    private suspend fun obtenerPieza(id: String): Pieza =
        database.piezaDao().obtenerPorId(id) ?: throw ErrorInventario("No se encontró la pieza.")

    private fun validarSolicitud(solicitud: SolicitudMovimiento): String? {
        if (solicitud.cantidad <= 0) {
            throw ErrorInventario("La cantidad debe ser un entero positivo.")
        }
        val motivo = solicitud.motivo?.trim()?.takeIf(String::isNotEmpty)
        if (motivo == null) {
            throw ErrorInventario("Escribe un motivo para este movimiento.")
        }
        if ((motivo?.length ?: 0) > Reglas.MAX_MOTIVO) {
            throw ErrorInventario("El motivo no puede superar ${Reglas.MAX_MOTIVO} caracteres.")
        }
        if (solicitud.tipo == TipoMovimientoInventario.AJUSTE && solicitud.sentidoAjuste == null) {
            throw ErrorInventario("Selecciona si el ajuste incrementa o disminuye la existencia.")
        }
        if (solicitud.tipo != TipoMovimientoInventario.AJUSTE && solicitud.sentidoAjuste != null) {
            throw ErrorInventario("El sentido solo se puede especificar para un ajuste.")
        }
        if (solicitud.tipo == TipoMovimientoInventario.RESERVA && solicitud.ordenId == null) {
            throw ErrorInventario("Selecciona la orden a la que se reserva la pieza.")
        }
        if (solicitud.tipo == TipoMovimientoInventario.LIBERACION &&
            solicitud.movimientoOrigenId == null
        ) {
            throw ErrorInventario("Selecciona la reserva que se liberará.")
        }
        if (solicitud.tipo == TipoMovimientoInventario.DEVOLUCION &&
            solicitud.movimientoOrigenId == null
        ) {
            throw ErrorInventario("Selecciona el consumo que se devolverá.")
        }
        if (solicitud.tipo != TipoMovimientoInventario.CONSUMO &&
            solicitud.tipo != TipoMovimientoInventario.LIBERACION &&
            solicitud.tipo != TipoMovimientoInventario.DEVOLUCION &&
            solicitud.movimientoOrigenId != null
        ) {
            throw ErrorInventario("El movimiento de origen no aplica a esta operación.")
        }
        if (solicitud.costoUnitarioMinor != null && solicitud.costoUnitarioMinor < 0) {
            throw ErrorInventario("El costo del movimiento no puede ser negativo.")
        }
        return motivo
    }

    private fun resolverOrigen(
        solicitud: SolicitudMovimiento,
        movimientos: List<MovimientoInventario>
    ): MovimientoInventario? {
        val origenId = solicitud.movimientoOrigenId ?: return null
        val origen = movimientos.firstOrNull { it.id == origenId && it.confirmadaEn != null }
            ?: throw ErrorInventario("No se encontró el movimiento de origen de esta pieza.")
        if (origen.piezaId != solicitud.piezaId) {
            throw ErrorInventario("El movimiento de origen pertenece a otra pieza.")
        }
        when (solicitud.tipo) {
            TipoMovimientoInventario.LIBERACION -> if (origen.tipo != "RESERVA") {
                throw ErrorInventario("Solo se puede liberar una reserva.")
            }
            TipoMovimientoInventario.DEVOLUCION -> if (origen.tipo != "CONSUMO") {
                throw ErrorInventario("La devolución debe referenciar un consumo.")
            }
            TipoMovimientoInventario.CONSUMO -> if (origen.tipo != "RESERVA") {
                throw ErrorInventario("El consumo reservado debe referenciar una reserva.")
            }
            else -> Unit
        }
        return origen
    }

    private suspend fun validarOrden(ordenId: String?, origen: MovimientoInventario?) {
        val id = ordenId ?: origen?.ordenId ?: return
        val orden = database.ordenServicioDao().obtenerPorId(id)
            ?.takeIf { it.eliminadaEn == null }
            ?: throw ErrorInventario("La orden seleccionada ya no está disponible.")
        if (origen?.ordenId != null && origen.ordenId != orden.id) {
            throw ErrorInventario("La operación debe conservar la orden de origen.")
        }
    }

    private fun validarDisponibilidad(
        solicitud: SolicitudMovimiento,
        saldo: SaldoInventario,
        origen: MovimientoInventario?,
        movimientos: List<MovimientoInventario>
    ) {
        val cantidad = solicitud.cantidad.toLong()
        if ((solicitud.tipo == TipoMovimientoInventario.LIBERACION ||
                (solicitud.tipo == TipoMovimientoInventario.CONSUMO && origen != null)) &&
            movimientos.any {
                it.confirmadaEn != null &&
                    (it.tipo == TipoMovimientoInventario.LIBERACION.codigo ||
                        it.tipo == TipoMovimientoInventario.CONSUMO.codigo) &&
                    it.movimientoOrigenId == null
            }
        ) {
            throw ErrorInventario(
                "Hay operaciones históricas sin vínculo a su reserva. No es seguro modificar una reserva desde este inventario."
            )
        }
        when (solicitud.tipo) {
            TipoMovimientoInventario.RESERVA -> if (cantidad > saldo.disponible) {
                throw ErrorInventario(
                    "Existencia insuficiente: hay ${saldo.disponible} piezas disponibles."
                )
            }
            TipoMovimientoInventario.CONSUMO -> {
                if (origen != null) {
                    val remanente = ReglasPiezas.remanenteReserva(origen.id, movimientos)
                    if (cantidad > remanente) {
                        throw ErrorInventario("La cantidad excede el remanente de la reserva.")
                    }
                } else if (cantidad > saldo.disponible) {
                    throw ErrorInventario(
                        "Existencia insuficiente: hay ${saldo.disponible} piezas disponibles."
                    )
                }
            }
            TipoMovimientoInventario.LIBERACION -> {
                val reserva = origen ?: throw ErrorInventario("Selecciona una reserva.")
                val remanente = ReglasPiezas.remanenteReserva(reserva.id, movimientos)
                if (cantidad > remanente) {
                    throw ErrorInventario("La cantidad excede el remanente de la reserva.")
                }
            }
            TipoMovimientoInventario.DEVOLUCION -> {
                val consumo = origen ?: throw ErrorInventario("Selecciona un consumo.")
                val devuelto = movimientos.asSequence()
                    .filter {
                        it.confirmadaEn != null &&
                            it.tipo == TipoMovimientoInventario.DEVOLUCION.codigo &&
                            it.movimientoOrigenId == consumo.id
                    }
                    .fold(0L) { total, movimiento ->
                        Math.addExact(total, movimiento.cantidad.toLong())
                    }
                if (Math.addExact(devuelto, cantidad) > consumo.cantidad.toLong()) {
                    throw ErrorInventario("La devolución excede la cantidad consumida.")
                }
            }
            TipoMovimientoInventario.AJUSTE -> {
                if (solicitud.sentidoAjuste == SentidoAjuste.DECREMENTO &&
                    cantidad > saldo.disponible
                ) {
                    throw ErrorInventario(
                        "El ajuste dejaría menos existencia que la cantidad reservada."
                    )
                }
            }
            TipoMovimientoInventario.ENTRADA -> Unit
        }
    }

    private fun validarCompensatorios(
        compensatorios: List<MovimientoInventario>,
        movimientos: List<MovimientoInventario>
    ) {
        val resultado = ReglasPiezas.calcularSaldos(movimientos + compensatorios)
        if (resultado.disponible < 0 || resultado.existenciaFisica < 0) {
            throw ErrorInventario("La compensación dejaría un saldo de inventario inválido.")
        }
    }

    private fun compensatorio(
        original: MovimientoInventario,
        tipo: TipoMovimientoInventario,
        instante: String,
        motivo: String,
        sentido: SentidoAjuste? = null
    ) = MovimientoInventario(
        id = EntidadId.nuevo(),
        piezaId = original.piezaId,
        ordenId = original.ordenId,
        tipo = tipo.codigo,
        cantidad = original.cantidad,
        costoUnitarioMinor = original.costoUnitarioMinor,
        confirmadaEn = instante,
        motivo = motivo,
        creadoEn = instante,
        sentidoAjuste = sentido?.codigo,
        movimientoOrigenId = original.id
    )

    private fun calcularSaldo(movimientos: List<MovimientoInventario>): SaldoInventario =
        try {
            ReglasPiezas.calcularSaldos(movimientos)
        } catch (exception: IllegalStateException) {
            throw ErrorInventario(exception.message ?: "El historial no permite calcular el saldo.")
        } catch (exception: IllegalArgumentException) {
            throw ErrorInventario(exception.message ?: "El historial de inventario es inconsistente.")
        } catch (exception: ArithmeticException) {
            throw ErrorInventario("El saldo excede el rango que puede representar la aplicación.")
        }

    private fun calcularSaldoParaConsulta(
        movimientos: List<MovimientoInventario>
    ): SaldoInventario = try {
        calcularSaldo(movimientos)
    } catch (_: DateTimeException) {
        SaldoInventario(0, 0, 0, requiereRevision = true)
    } catch (_: ErrorInventario) {
        SaldoInventario(0, 0, 0, requiereRevision = true)
    }

    private object Reglas {
        const val MAX_MOTIVO = 500
    }
}
