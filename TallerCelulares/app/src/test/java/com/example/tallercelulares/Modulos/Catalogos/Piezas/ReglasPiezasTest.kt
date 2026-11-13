package com.example.tallercelulares.Modulos.Catalogos.Piezas

import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasPiezasTest {
    @Test
    fun validarPiezaAdmiteIdentificadoresOpcionalesYExigeDatosDescriptivos() {
        val errores = ReglasPiezas.validarPieza(
            CamposPieza(
                codigo = "  ",
                numeroParte = "",
                nombre = "Pantalla OLED",
                variante = "Negra",
                calidad = "Original",
                costo = "120.00",
                precioVenta = "180.00",
                existenciaMinima = "2"
            ),
            "MXN"
        )

        assertTrue(errores.isEmpty())
        assertNull(ReglasPiezas.normalizarOpcional("  "))
        assertEquals("ABC", ReglasPiezas.normalizarOpcional(" ABC "))
    }

    @Test
    fun validaMonedaConPrecisionExactaSinUsarRedondeo() {
        assertEquals(12_345L, ReglasPiezas.precioMinor("123.45", "MXN"))
        assertEquals(123L, ReglasPiezas.precioMinor("123", "JPY"))
        assertNull(ReglasPiezas.precioMinor("123.5", "JPY"))
        assertNull(ReglasPiezas.precioMinor("-1.00", "MXN"))
        assertNull(ReglasPiezas.precioMinor("1,000.00", "MXN"))
    }

    @Test
    fun calculaStockReservaConsumoYLiberacionSinDependerDelOrdenDeLectura() {
        val movimientos = listOf(
            movimiento("lib", "LIBERACION", 1, origen = "res", fecha = "2026-11-12T12:00:04Z"),
            movimiento("cons", "CONSUMO", 3, origen = "res", fecha = "2026-11-12T12:00:03Z"),
            movimiento("res", "RESERVA", 7, orden = "orden", fecha = "2026-11-12T12:00:02Z"),
            movimiento("entrada", "ENTRADA", 10, fecha = "2026-11-12T12:00:01Z")
        )

        val saldo = ReglasPiezas.calcularSaldos(movimientos)

        assertEquals(7L, saldo.existenciaFisica)
        assertEquals(3L, saldo.cantidadReservada)
        assertEquals(4L, saldo.disponible)
        assertEquals(3L, ReglasPiezas.remanenteReserva("res", movimientos))
    }

    @Test(expected = IllegalArgumentException::class)
    fun noPermiteLiberarMasDeLoReservado() {
        ReglasPiezas.calcularSaldos(
            listOf(
                movimiento("entrada", "ENTRADA", 4, fecha = "2026-11-12T12:00:01Z"),
                movimiento("res", "RESERVA", 2, orden = "orden", fecha = "2026-11-12T12:00:02Z"),
                movimiento("lib", "LIBERACION", 3, origen = "res", fecha = "2026-11-12T12:00:03Z")
            )
        )
    }

    @Test
    fun elSentidoDelAjusteSeAplicaAlSaldoFisico() {
        val saldo = ReglasPiezas.calcularSaldos(
            listOf(
                movimiento("entrada", "ENTRADA", 10, fecha = "2026-11-12T12:00:01Z"),
                movimiento(
                    "ajuste-negativo",
                    "AJUSTE",
                    3,
                    sentido = "DECREMENTO",
                    fecha = "2026-11-12T12:00:02Z"
                ),
                movimiento(
                    "ajuste-positivo",
                    "AJUSTE",
                    2,
                    sentido = "INCREMENTO",
                    fecha = "2026-11-12T12:00:03Z"
                )
            )
        )

        assertEquals(9L, saldo.existenciaFisica)
        assertEquals(9L, saldo.disponible)
    }

    @Test(expected = IllegalArgumentException::class)
    fun noPermiteQueUnaEntradaPosteriorJustifiqueUnConsumoAnteriorSinSaldo() {
        ReglasPiezas.calcularSaldos(
            listOf(
                movimiento("consumo", "CONSUMO", 2, fecha = "2026-11-12T12:00:01Z"),
                movimiento("entrada", "ENTRADA", 4, fecha = "2026-11-12T12:00:02Z")
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun noPermiteDevolverMasDeLaCantidadConsumida() {
        ReglasPiezas.calcularSaldos(
            listOf(
                movimiento("entrada", "ENTRADA", 4, fecha = "2026-11-12T12:00:01Z"),
                movimiento("consumo", "CONSUMO", 2, fecha = "2026-11-12T12:00:02Z"),
                movimiento(
                    "devolucion",
                    "DEVOLUCION",
                    3,
                    origen = "consumo",
                    fecha = "2026-11-12T12:00:03Z"
                )
            )
        )
    }

    @Test(expected = IllegalStateException::class)
    fun noAdivinaElSentidoDeUnAjusteHistorico() {
        ReglasPiezas.calcularSaldos(listOf(movimiento("legacy", "AJUSTE", 2)))
    }

    private fun movimiento(
        id: String,
        tipo: String,
        cantidad: Int,
        origen: String? = null,
        orden: String? = null,
        sentido: String? = null,
        fecha: String = "2026-11-12T12:00:00Z"
    ) = MovimientoInventario(
        id = id,
        piezaId = "pieza",
        ordenId = orden,
        tipo = tipo,
        cantidad = cantidad,
        confirmadaEn = fecha,
        creadoEn = fecha,
        movimientoOrigenId = origen,
        sentidoAjuste = sentido
    )
}
