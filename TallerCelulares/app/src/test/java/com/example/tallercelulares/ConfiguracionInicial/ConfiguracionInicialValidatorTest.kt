package com.example.tallercelulares.ConfiguracionInicial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfiguracionInicialValidatorTest {
    @Test
    fun validaConfiguracionYConservaDatosOpcionalesComoNulos() {
        val resultado = ConfiguracionInicialValidator.validar(datosValidos())

        assertTrue(resultado is ResultadoValidacionTaller.Valido)
        val configuracion = (resultado as ResultadoValidacionTaller.Valido).configuracion
        assertNull(configuracion.nombre)
        assertNull(configuracion.telefono)
        assertEquals("MXN", configuracion.monedaCodigo)
        assertEquals("0.16", configuracion.impuestoTasa)
        assertEquals(12_345L, configuracion.cargoDiagnosticoMinor)
        assertEquals(1L, configuracion.siguienteFolio)
    }

    @Test
    fun rechazaMonedaInvalida() {
        val resultado = ConfiguracionInicialValidator.validar(datosValidos().copy(monedaCodigo = "ZZZ"))

        assertTrue(resultado is ResultadoValidacionTaller.Invalido)
        assertEquals("moneda", (resultado as ResultadoValidacionTaller.Invalido).campo)
    }

    @Test
    fun rechazaCargoConPrecisionMayorALaDeLaMoneda() {
        val resultado = ConfiguracionInicialValidator.validar(
            datosValidos().copy(cargoDiagnostico = "123.456")
        )

        assertTrue(resultado is ResultadoValidacionTaller.Invalido)
        assertEquals("cargo_diagnostico", (resultado as ResultadoValidacionTaller.Invalido).campo)
    }

    @Test
    fun rechazaVigenciaCero() {
        val resultado = ConfiguracionInicialValidator.validar(
            datosValidos().copy(diasVigenciaCotizacion = "0")
        )

        assertTrue(resultado is ResultadoValidacionTaller.Invalido)
        assertEquals("vigencia", (resultado as ResultadoValidacionTaller.Invalido).campo)
    }

    private fun datosValidos() = DatosTaller(
        nombre = "",
        telefono = "",
        direccion = "",
        monedaCodigo = "mxn",
        zonaHoraria = "America/Mexico_City",
        prefijoFolio = "TC",
        siguienteFolio = "1",
        impuestoPorcentaje = "16",
        cargoDiagnostico = "123.45",
        diasVigenciaCotizacion = "7",
        diasGarantia = "30",
        diasSeguimientoAutorizacion = "3",
        diasRefaccionAtrasada = "3",
        diasEquipoListo = "2"
    )
}
