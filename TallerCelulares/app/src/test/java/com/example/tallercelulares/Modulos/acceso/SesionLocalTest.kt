package com.example.tallercelulares.Modulos.acceso

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SesionLocalTest {
    @Test
    fun unaSesionNuevaPermaneceBloqueada() {
        val session = SesionLocal(tiempoBloqueoMillis = 300_000L)

        assertFalse(session.estaAutenticada)
        assertTrue(session.haExpirado(elapsedRealtime = 10_000L))
    }

    @Test
    fun actividadReiniciaElTiempoDeInactividad() {
        val session = SesionLocal(tiempoBloqueoMillis = 300_000L)
        session.iniciarAhora(elapsedRealtime = 1_000L)
        session.registrarActividad(elapsedRealtime = 250_000L)

        assertFalse(session.haExpirado(elapsedRealtime = 549_999L))
        assertTrue(session.haExpirado(elapsedRealtime = 550_000L))
    }

    @Test
    fun bloquearInvalidaLaSesion() {
        val session = SesionLocal(tiempoBloqueoMillis = 300_000L)
        session.iniciarAhora(elapsedRealtime = 1_000L)

        session.bloquear()

        assertFalse(session.estaAutenticada)
        assertTrue(session.haExpirado(elapsedRealtime = 1_001L))
    }

    @Test
    fun relojAnteriorNoExtiendeLaSesion() {
        val session = SesionLocal(tiempoBloqueoMillis = 300_000L)
        session.iniciarAhora(elapsedRealtime = 100_000L)

        assertTrue(session.haExpirado(elapsedRealtime = 99_000L))
    }
}
