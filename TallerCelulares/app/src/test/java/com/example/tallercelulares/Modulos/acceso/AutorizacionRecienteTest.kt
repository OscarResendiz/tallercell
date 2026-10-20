package com.example.tallercelulares.Modulos.acceso

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutorizacionRecienteTest {
    @Test
    fun autorizacionSoloSirveParaLaAccionSolicitada() {
        val request = SolicitudAutenticacionReciente.crear(AccionSensible.BORRAR_DATOS)
        val authorization = AutorizacionReciente(
            accion = AccionSensible.BORRAR_DATOS,
            idSolicitud = request.idSolicitud,
            autorizadaEnElapsedRealtime = 1_000L
        )
        val otherAction = SolicitudAutenticacionReciente(
            accion = AccionSensible.EXPORTAR_RESPALDO,
            idSolicitud = request.idSolicitud
        )

        assertFalse(
            ConsumidorAutorizacionReciente.consumir(
                authorization,
                otherAction,
                ahoraElapsedRealtime = 1_001L
            )
        )
        assertTrue(
            ConsumidorAutorizacionReciente.consumir(
                authorization,
                request,
                ahoraElapsedRealtime = 1_001L
            )
        )
    }

    @Test
    fun autorizacionExpiradaONoCoincidenteSeRechaza() {
        val request = SolicitudAutenticacionReciente.crear(AccionSensible.EXPORTAR_RESPALDO)
        val expired = AutorizacionReciente(
            accion = request.accion,
            idSolicitud = request.idSolicitud,
            autorizadaEnElapsedRealtime = 1_000L
        )

        assertFalse(
            ConsumidorAutorizacionReciente.consumir(
                expired,
                request,
                ahoraElapsedRealtime = 31_001L
            )
        )
    }

    @Test
    fun unaAutorizacionNoPuedeConsumirseDosVeces() {
        val request = SolicitudAutenticacionReciente.crear(AccionSensible.RESTAURAR_RESPALDO)
        val authorization = AutorizacionReciente(
            accion = request.accion,
            idSolicitud = request.idSolicitud,
            autorizadaEnElapsedRealtime = 5_000L
        )

        assertTrue(
            ConsumidorAutorizacionReciente.consumir(
                authorization,
                request,
                ahoraElapsedRealtime = 5_001L
            )
        )
        assertFalse(
            ConsumidorAutorizacionReciente.consumir(
                authorization,
                request,
                ahoraElapsedRealtime = 5_002L
            )
        )
    }
}
