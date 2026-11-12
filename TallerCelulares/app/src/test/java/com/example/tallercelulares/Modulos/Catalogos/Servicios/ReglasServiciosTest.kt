package com.example.tallercelulares.Modulos.Catalogos.Servicios

import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasServiciosTest {
    @Test
    fun validarAceptaDescripcionNombreYPrecioConCodigoOpcional() {
        val errores = ReglasServicios.validar(
            codigo = "  ",
            nombre = "Cambio de pantalla",
            descripcion = "Reemplazo de módulo y prueba funcional.",
            precio = "1250.50",
            monedaCodigo = "MXN"
        )

        assertTrue(errores.isEmpty())
        assertNull(ReglasServicios.normalizarCodigo("   "))
    }

    @Test
    fun validarRequiereNombreDescripcionYPrecio() {
        val errores = ReglasServicios.validar(
            codigo = "",
            nombre = " ",
            descripcion = " ",
            precio = "-2.00",
            monedaCodigo = "MXN"
        )

        assertTrue(errores.containsKey("nombre"))
        assertTrue(errores.containsKey("descripcion"))
        assertTrue(errores.containsKey("precio"))
    }

    @Test
    fun convierteImportesConPrecisionExactaDeLaMoneda() {
        assertEquals(12_345L, ReglasServicios.precioBaseMinor("123.45", "MXN"))
        assertEquals(123L, ReglasServicios.precioBaseMinor("123", "JPY"))
        assertNull(ReglasServicios.precioBaseMinor("123.5", "JPY"))
        assertNull(ReglasServicios.precioBaseMinor("1,000.00", "MXN"))
    }

    @Test
    fun calculaSubtotalImpuestoYTotalSinPerderPrecision() {
        val detalles = listOf(
            detalle(1, precio = 1_000L),
            detalle(2, precio = 1_250L)
        )

        val totales = ReglasServicios.calcularTotales(
            detalles = detalles,
            descuentoMinor = 0,
            impuestoTasa = "0.16"
        )

        assertEquals(3_500L, totales.subtotalMinor)
        assertEquals(560L, totales.impuestoMinor)
        assertEquals(4_060L, totales.totalMinor)
    }

    private fun detalle(cantidad: Int, precio: Long) = DetalleCotizacion(
        cotizacionId = "quote",
        servicioId = "service",
        tipo = "MANO_DE_OBRA",
        descripcionSnapshot = "Servicio",
        cantidad = cantidad,
        precioUnitarioMinor = precio,
        importeMinor = Math.multiplyExact(cantidad.toLong(), precio)
    )
}
