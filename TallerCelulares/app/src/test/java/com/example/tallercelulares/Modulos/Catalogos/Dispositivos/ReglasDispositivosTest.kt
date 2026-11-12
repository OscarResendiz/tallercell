package com.example.tallercelulares.Modulos.Catalogos.Dispositivos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasDispositivosTest {
    @Test
    fun validarAceptaIdentificadoresOpcionalesYTextoUnicode() {
        val errores = ReglasDispositivos.validar(
            tipo = " Teléfono ",
            marca = "Xiaomi",
            modelo = "Redmi Note 13",
            color = "Azul océano",
            numeroSerie = "",
            imei = "  "
        )

        assertTrue(errores.isEmpty())
        val datos = ReglasDispositivos.normalizar(
            tipo = " Teléfono ",
            marca = " Xiaomi ",
            modelo = " Redmi Note 13 ",
            color = " Azul océano ",
            numeroSerie = "",
            imei = "  "
        )
        assertEquals("Teléfono", datos.tipo)
        assertEquals("Xiaomi", datos.marca)
        assertNull(datos.numeroSerie)
        assertNull(datos.imei)
    }

    @Test
    fun validarRechazaCamposRequeridosVaciosYLargos() {
        val errores = ReglasDispositivos.validar(
            tipo = "  ",
            marca = "X".repeat(101),
            modelo = "Modelo",
            color = "",
            numeroSerie = "",
            imei = ""
        )

        assertTrue(errores.containsKey("tipo"))
        assertTrue(errores.containsKey("marca"))
        assertFalse(errores.containsKey("color"))
    }

    @Test
    fun identificadorEnmascaradoNuncaDevuelveElValorCompleto() {
        assertEquals("••••••7890", ReglasDispositivos.identificadorEnmascarado("  1234567890 "))
        assertEquals("••••", ReglasDispositivos.identificadorEnmascarado("1234"))
        assertEquals("No registrado", ReglasDispositivos.identificadorEnmascarado(null))
    }

    @Test
    fun patronBusquedaEscapaComodinesSql() {
        assertEquals("a\\%b\\_c\\\\d", ReglasDispositivos.patronBusqueda("a%b_c\\d"))
    }
}
