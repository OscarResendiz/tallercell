package com.example.tallercelulares.Modulos.Catalogos.Clientes

import java.text.Normalizer
import java.util.Locale

internal object ReglasClientes {
    private val correoValido = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizarBusqueda(valor: String): String = Normalizer
        .normalize(valor, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")

    fun digitosTelefono(valor: String): String = valor.filter(Char::isDigit)

    fun patronBusqueda(valor: String): String = normalizarBusqueda(valor)
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    fun textoOpcional(valor: String): String? =
        valor.trim().takeIf(String::isNotEmpty)

    fun validar(
        nombre: String,
        telefono: String,
        correo: String,
        notas: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        val nombreLimpio = nombre.trim()
        val telefonoLimpio = textoOpcional(telefono)
        val correoLimpio = textoOpcional(correo)
        val notasLimpias = textoOpcional(notas)

        if (nombreLimpio.isEmpty()) {
            errores["nombre"] = "El nombre es obligatorio."
        } else if (nombreLimpio.length > MAX_NOMBRE) {
            errores["nombre"] = "El nombre no puede superar $MAX_NOMBRE caracteres."
        }
        if (telefonoLimpio != null && telefonoLimpio.length > MAX_TELEFONO) {
            errores["telefono"] = "El teléfono no puede superar $MAX_TELEFONO caracteres."
        }
        if (correoLimpio != null && !correoValido.matches(correoLimpio)) {
            errores["correo"] = "Escribe un correo válido o deja el campo vacío."
        } else if (correoLimpio != null && correoLimpio.length > MAX_CORREO) {
            errores["correo"] = "El correo no puede superar $MAX_CORREO caracteres."
        }
        if (notasLimpias != null && notasLimpias.length > MAX_NOTAS) {
            errores["notas"] = "Las notas no pueden superar $MAX_NOTAS caracteres."
        }
        return errores
    }

    const val MAX_NOMBRE = 120
    const val MAX_TELEFONO = 40
    const val MAX_CORREO = 254
    const val MAX_NOTAS = 1_000
}
