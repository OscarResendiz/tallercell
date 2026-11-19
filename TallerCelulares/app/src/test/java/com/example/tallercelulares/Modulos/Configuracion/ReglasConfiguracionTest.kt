package com.example.tallercelulares.Modulos.Configuracion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasConfiguracionTest {
    @Test
    fun aceptaConfiguracionValida() {
        val resultado = ReglasConfiguracion.validar(camposValidos())

        assertTrue(resultado.isEmpty())
        val convertida = ReglasConfiguracion.convertir(camposValidos())
        assertEquals("MXN", convertida?.monedaCodigo)
        assertEquals("0.16", convertida?.impuestoTasa)
        assertEquals(12_345L, convertida?.cargoDiagnosticoMinor)
    }

    @Test
    fun rechazaMonedaZonaYPrefijoInvalidos() {
        val resultado = ReglasConfiguracion.validar(
            camposValidos().copy(
                monedaCodigo = "ZZZ",
                zonaHoraria = "Zona/Inexistente",
                prefijoFolio = "prefijo demasiado largo"
            )
        )

        assertTrue(resultado.containsKey("moneda"))
        assertTrue(resultado.containsKey("zona_horaria"))
        assertTrue(resultado.containsKey("prefijo_folio"))
    }

    @Test
    fun rechazaImpuestoYCargoFueraDeRango() {
        val resultado = ReglasConfiguracion.validar(
            camposValidos().copy(
                impuestoPorcentaje = "101",
                cargoDiagnostico = "-1"
            )
        )

        assertTrue(resultado.containsKey("impuesto"))
        assertTrue(resultado.containsKey("cargo_diagnostico"))
    }

    @Test
    fun rechazaDiasFueraDeRango() {
        val resultado = ReglasConfiguracion.validar(
            camposValidos().copy(diasVigenciaCotizacion = "0")
        )

        assertEquals(
            "El plazo debe ser de 1 a 3650 días.",
            resultado["vigencia"]
        )
    }

    @Test
    fun validaNombreDeMetodoDePago() {
        assertNull(ReglasConfiguracion.validarMetodoPago("Transferencia"))
        assertTrue(ReglasConfiguracion.validarMetodoPago("") != null)
    }

    private fun camposValidos() = CamposConfiguracionTaller(
        nombre = " Taller Local ",
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
        diasEquipoListo = "2",
        textoRecepcion = "",
        textoAutorizacion = "",
        textoGarantia = ""
    )
}
