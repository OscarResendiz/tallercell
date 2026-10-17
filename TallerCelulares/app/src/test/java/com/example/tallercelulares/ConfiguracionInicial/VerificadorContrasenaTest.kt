package com.example.tallercelulares.ConfiguracionInicial

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerificadorContrasenaTest {
    @Test
    fun contrasenaCorrectaProduceVerificadorVerificableConSalAleatoria() {
        val password = "TallerSegur0!".toCharArray()
        val first = VerificadorContrasena.derivar(password)
        val second = VerificadorContrasena.derivar(password)

        assertNotEquals(first.sal.toList(), second.sal.toList())
        assertTrue(
            VerificadorContrasena.verificar(
                password,
                first.verificador,
                first.sal,
                first.algoritmo
            )
        )
        assertFalse(
            VerificadorContrasena.verificar(
                "OtraContrasena!".toCharArray(),
                first.verificador,
                first.sal,
                first.algoritmo
            )
        )
        assertTrue(first.verificador.size == VerificadorContrasena.LONGITUD_BITS / 8)
        password.fill('\u0000')
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaContrasenaDemasiadoCorta() {
        VerificadorContrasena.derivar("corta".toCharArray())
    }
}
