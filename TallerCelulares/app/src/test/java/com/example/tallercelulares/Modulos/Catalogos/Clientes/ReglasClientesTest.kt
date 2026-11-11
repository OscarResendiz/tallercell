package com.example.tallercelulares.Modulos.Catalogos.Clientes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasClientesTest {
    @Test
    fun normalizaBusquedaSinAcentosYEspaciosAdicionales() {
        assertEquals("jose alvarez", ReglasClientes.normalizarBusqueda("  José   Álvarez "))
    }

    @Test
    fun obtieneDigitosParaCompararTelefonosConDistintoFormato() {
        assertEquals("525551234567", ReglasClientes.digitosTelefono("+52 (555) 123-4567"))
    }

    @Test
    fun escapaComodinesDeBusquedaSinCambiarLaConsultaNormalizada() {
        assertEquals("\\%\\_\\\\", ReglasClientes.patronBusqueda("%_\\"))
    }

    @Test
    fun permiteNombreSinDatosOpcionalesYNormalizaTextoVacio() {
        assertTrue(ReglasClientes.validar("  Ana María  ", "", "", "  ").isEmpty())
        assertEquals(null, ReglasClientes.textoOpcional("   "))
    }

    @Test
    fun rechazaNombreVacioYCorreoInvalidoSinHacerUnicoElTelefono() {
        val errores = ReglasClientes.validar(" ", "", "correo-invalido", "")
        assertTrue(errores.containsKey("nombre"))
        assertTrue(errores.containsKey("correo"))
        assertFalse(
            ReglasClientes.validar("Ana", "+52 555 123", "", "").containsKey("telefono")
        )
    }
}
