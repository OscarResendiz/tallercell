package com.example.tallercelulares.Modulos.tablero

import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenTableroRegistro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReglasTableroTest {
    @Test
    fun normalizaBusquedaYBuscaDatosOpcionalesSinExponerlosEnPresentacion() {
        val orden = orden(
            cliente = "José Álvarez",
            contacto = "+52 (555) 123-4567",
            imei = "351234567890123"
        )

        assertTrue(ReglasTablero.coincideBusqueda(orden, "jose alvarez"))
        assertTrue(ReglasTablero.coincideBusqueda(orden, "555 123"))
        assertTrue(ReglasTablero.coincideBusqueda(orden, "351234567890123"))
        assertFalse(ReglasTablero.coincideBusqueda(orden.copy(imei = null), "351234567890123"))
    }

    @Test
    fun marcaPromesaVencidaSinModificarLaOrden() {
        val ordenOriginal = orden(
            fechaPrometida = "2026-10-18T12:00:00Z",
            estado = "EN_REPARACION"
        )

        val tarjeta = ReglasTablero.crearTarjeta(
            orden = ordenOriginal,
            ahora = Instant.parse("2026-10-19T12:00:00Z"),
            zonaHoraria = ZoneId.of("America/Mexico_City"),
            diasSeguimientoAutorizacion = 3,
            diasRefaccionAtrasada = 4,
            diasEquipoListo = 5
        )

        assertEquals("EN_REPARACION", ordenOriginal.estado)
        assertEquals("2026-10-18T12:00:00Z", ordenOriginal.fechaPrometida)
        assertEquals("Promesa vencida", tarjeta.alerta?.titulo)
        assertEquals("Revisar el avance de la reparación", tarjeta.proximaAccion)
    }

    @Test
    fun aplicaSoloLosPlazosDeSeguimientoConfigurados() {
        val esperando = orden(
            estado = "ESPERANDO_REFACCION",
            estadoDesde = "2026-10-10T00:00:00Z"
        )
        val ahora = Instant.parse("2026-10-19T00:00:00Z")

        val sinPlazo = ReglasTablero.crearTarjeta(
            esperando, ahora, ZoneId.of("UTC"), 3, 0, 5
        )
        val conPlazoVencido = ReglasTablero.crearTarjeta(
            esperando, ahora, ZoneId.of("UTC"), 3, 7, 5
        )

        assertNull(sinPlazo.alerta)
        assertEquals("Seguimiento recomendado", conPlazoVencido.alerta?.titulo)
        assertTrue(conPlazoVencido.alerta?.detalle?.contains("9 días") == true)
    }

    @Test
    fun noMarcaEstadosTerminalesAunqueLaFechaPrometidaHayaVencido() {
        val entregada = ReglasTablero.crearTarjeta(
            orden(estado = "ENTREGADA", fechaPrometida = "2026-10-01T00:00:00Z"),
            Instant.parse("2026-10-19T00:00:00Z"),
            ZoneId.of("UTC"),
            diasSeguimientoAutorizacion = 1,
            diasRefaccionAtrasada = 1,
            diasEquipoListo = 1
        )

        assertNull(entregada.alerta)
        assertEquals("Sin acción operativa pendiente", entregada.proximaAccion)
        assertNotNull(entregada.antiguedad)
    }

    @Test
    fun estadoDesconocidoUsaEtiquetaYAccionNeutras() {
        assertEquals("Estado: NUEVO_ESTADO", ReglasTablero.etiquetaEstado("NUEVO_ESTADO"))
        assertEquals("Revisar orden", ReglasTablero.proximaAccion("NUEVO_ESTADO"))
        assertNull(ReglasTablero.grupo("NUEVO_ESTADO"))
    }

    private fun orden(
        estado: String = "EN_REPARACION",
        cliente: String = "Cliente de prueba",
        contacto: String? = null,
        imei: String? = null,
        fechaPrometida: String? = null,
        estadoDesde: String = "2026-10-18T00:00:00Z"
    ) = OrdenTableroRegistro(
        id = "orden-1",
        folio = "TC-0001",
        cliente = cliente,
        contacto = contacto,
        tipoEquipo = "Teléfono",
        marca = "Marca",
        modelo = "Modelo",
        serie = null,
        imei = imei,
        estado = estado,
        recibidaEn = "2026-10-17T00:00:00Z",
        fechaPrometida = fechaPrometida,
        estadoDesde = estadoDesde
    )
}
