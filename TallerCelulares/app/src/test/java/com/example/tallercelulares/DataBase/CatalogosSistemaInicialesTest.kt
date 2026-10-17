package com.example.tallercelulares.DataBase

import com.example.tallercelulares.DataBase.Entidades.CatalogosSistema.CatalogosSistemaIniciales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogosSistemaInicialesTest {
    @Test
    fun monedasIncluyeOpcionesComunesYLaMonedaDelDispositivo() {
        val monedas = CatalogosSistemaIniciales.monedas("MXN")

        assertTrue(monedas.size > 1)
        assertTrue(monedas.any { it.codigo == "MXN" })
        assertTrue(monedas.any { it.codigo == "USD" })
        assertTrue(monedas.any { it.codigo == "EUR" })
        assertEquals(monedas.size, monedas.map { it.codigo }.distinct().size)
    }

    @Test
    fun zonasHorariasIncluyeOpcionesComunesYLaZonaDelDispositivo() {
        val zonas = CatalogosSistemaIniciales.zonasHorarias("America/Mexico_City")

        assertTrue(zonas.size > 1)
        assertTrue(zonas.any { it.id == "America/Mexico_City" && it.nombre == "Ciudad de México" })
        assertTrue(zonas.any { it.id == "America/New_York" })
        assertTrue(zonas.any { it.id == "Europe/Madrid" })
        assertTrue(zonas.any { it.id == "UTC" })
        assertEquals(zonas.size, zonas.map { it.id }.distinct().size)
    }

    @Test
    fun mantieneEnCatalogoUnValorExistenteAunqueNoEsteEnLaListaComun() {
        val monedas = CatalogosSistemaIniciales.monedas("MXN", "ISK")
        val zonas = CatalogosSistemaIniciales.zonasHorarias("America/Mexico_City", "Pacific/Chatham")

        assertTrue(monedas.any { it.codigo == "ISK" })
        assertTrue(zonas.any { it.id == "Pacific/Chatham" })
    }
}
