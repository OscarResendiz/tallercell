package com.example.tallercelulares.Modulos.acceso

/**
 * Estado efímero de la sesión. Nunca se restaura desde preferencias o desde el estado de la Activity.
 */
internal class SesionLocal(private val tiempoBloqueoMillis: Long) {
    private var ultimoUsoMillis: Long? = null

    val estaAutenticada: Boolean
        get() = ultimoUsoMillis != null

    fun iniciarAhora(elapsedRealtime: Long) {
        ultimoUsoMillis = elapsedRealtime
    }

    fun registrarActividad(elapsedRealtime: Long) {
        if (estaAutenticada) ultimoUsoMillis = elapsedRealtime
    }

    fun haExpirado(elapsedRealtime: Long): Boolean {
        val ultimoUso = ultimoUsoMillis ?: return true
        if (elapsedRealtime < ultimoUso) return true
        return elapsedRealtime - ultimoUso >= tiempoBloqueoMillis
    }

    fun bloquear() {
        ultimoUsoMillis = null
    }
}
