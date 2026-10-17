package com.example.tallercelulares.ConfiguracionInicial

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class VerificadorDerivado(
    val verificador: ByteArray,
    val sal: ByteArray,
    val algoritmo: String
)

object VerificadorContrasena {
    const val ALGORITMO = "PBKDF2WithHmacSHA256"
    const val ITERACIONES = 210_000
    const val LONGITUD_BITS = 256
    const val LONGITUD_SAL_BYTES = 16

    fun derivar(contrasena: CharArray): VerificadorDerivado {
        require(contrasena.size in 10..128) { "La contraseña debe tener entre 10 y 128 caracteres." }
        val sal = ByteArray(LONGITUD_SAL_BYTES).also(SecureRandom()::nextBytes)
        return VerificadorDerivado(
            verificador = calcular(contrasena, sal),
            sal = sal,
            algoritmo = "$ALGORITMO:$ITERACIONES:$LONGITUD_BITS"
        )
    }

    fun verificar(contrasena: CharArray, verificador: ByteArray, sal: ByteArray, algoritmo: String): Boolean {
        if (algoritmo != "$ALGORITMO:$ITERACIONES:$LONGITUD_BITS" || sal.size != LONGITUD_SAL_BYTES) {
            return false
        }
        if (contrasena.size !in 10..128 || verificador.size != LONGITUD_BITS / 8) return false
        val calculado = calcular(contrasena, sal)
        return MessageDigest.isEqual(calculado, verificador)
    }

    private fun calcular(contrasena: CharArray, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(contrasena, sal, ITERACIONES, LONGITUD_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
