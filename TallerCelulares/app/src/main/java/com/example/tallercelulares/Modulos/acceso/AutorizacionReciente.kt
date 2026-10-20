package com.example.tallercelulares.Modulos.acceso

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import android.os.SystemClock
import java.util.UUID

/** Acciones que exigen volver a comprobar la identidad antes de ejecutarse. */
enum class AccionSensible(val codigo: String) {
    VER_CREDENCIAL_EQUIPO("ver_credencial_equipo"),
    EXPORTAR_RESPALDO("exportar_respaldo"),
    RESTAURAR_RESPALDO("restaurar_respaldo"),
    BORRAR_DATOS("borrar_datos");

    companion object {
        internal fun desdeCodigo(codigo: String?): AccionSensible? =
            entries.firstOrNull { it.codigo == codigo }
    }
}

/** Solicitud ligada a una sola acción y a una instancia concreta del diálogo de autenticación. */
class SolicitudAutenticacionReciente internal constructor(
    val accion: AccionSensible,
    val idSolicitud: String
) {
    companion object {
        fun crear(accion: AccionSensible): SolicitudAutenticacionReciente =
            SolicitudAutenticacionReciente(accion, UUID.randomUUID().toString())
    }
}

/** Resultado de autenticación que solo se acepta durante un intervalo breve y una sola vez. */
class AutorizacionReciente internal constructor(
    val accion: AccionSensible,
    val idSolicitud: String,
    val autorizadaEnElapsedRealtime: Long
) {
    internal fun esValidaPara(
        solicitud: SolicitudAutenticacionReciente,
        ahoraElapsedRealtime: Long
    ): Boolean =
        accion == solicitud.accion &&
            idSolicitud == solicitud.idSolicitud &&
            ahoraElapsedRealtime >= autorizadaEnElapsedRealtime &&
            ahoraElapsedRealtime - autorizadaEnElapsedRealtime <= VIGENCIA_MILLIS

    private companion object {
        const val VIGENCIA_MILLIS = 30_000L
    }
}

/**
 * Verifica y consume el resultado inmediatamente antes de ejecutar su acción.
 * El identificador impide reutilizar una autorización ya consumida.
 */
object ConsumidorAutorizacionReciente {
    private val consumidas = linkedMapOf<String, Long>()

    @Synchronized
    fun consumir(
        autorizacion: AutorizacionReciente?,
        solicitud: SolicitudAutenticacionReciente,
        ahoraElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        consumidas.entries.removeAll { ahoraElapsedRealtime - it.value > AUTORIZACION_VIGENCIA_MILLIS }
        if (autorizacion == null || !autorizacion.esValidaPara(solicitud, ahoraElapsedRealtime)) {
            return false
        }
        if (consumidas.containsKey(autorizacion.idSolicitud)) return false
        consumidas[autorizacion.idSolicitud] = autorizacion.autorizadaEnElapsedRealtime
        return true
    }

    private const val AUTORIZACION_VIGENCIA_MILLIS = 30_000L
}

/** Activity Result API para solicitar autenticación reciente desde cualquier módulo protegido. */
class ContratoAutenticacionReciente :
    ActivityResultContract<SolicitudAutenticacionReciente, AutorizacionReciente?>() {

    override fun createIntent(
        context: Context,
        input: SolicitudAutenticacionReciente
    ): Intent = AccesoActivity.crearIntentAutenticacion(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): AutorizacionReciente? {
        if (resultCode != android.app.Activity.RESULT_OK || intent == null) return null
        val action = AccionSensible.desdeCodigo(intent.getStringExtra(AccesoActivity.EXTRA_ACCION))
            ?: return null
        val requestId = intent.getStringExtra(AccesoActivity.EXTRA_ID_SOLICITUD) ?: return null
        val authorizedAt = intent.getLongExtra(AccesoActivity.EXTRA_AUTORIZADA_EN, -1L)
        if (authorizedAt < 0L) return null
        return AutorizacionReciente(action, requestId, authorizedAt)
    }
}
