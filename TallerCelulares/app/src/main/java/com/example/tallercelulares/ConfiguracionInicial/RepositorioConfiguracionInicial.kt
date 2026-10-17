package com.example.tallercelulares.ConfiguracionInicial

import androidx.room.withTransaction
import com.example.tallercelulares.DataBase.TallerCelularesDatabase
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal.ConfiguracionAccesoLocal
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTaller
import com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema.CatalogoMonedaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema.CatalogoZonaHorariaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogosSistema.CatalogosSistemaIniciales
import java.time.Instant

enum class EstadoConfiguracionInicial {
    SIN_CONFIGURACION,
    INCOMPLETA,
    COMPLETA
}

class RepositorioConfiguracionInicial(private val database: TallerCelularesDatabase) {
    suspend fun estado(): EstadoConfiguracionInicial {
        val taller = database.configuracionTallerDao().obtenerPorId(ID_TALLER)
        val acceso = database.configuracionAccesoLocalDao().obtenerPorId(ID_ACCESO)
        return when {
            taller == null && acceso == null -> EstadoConfiguracionInicial.SIN_CONFIGURACION
            taller != null && acceso != null && accesoValido(acceso) -> EstadoConfiguracionInicial.COMPLETA
            else -> EstadoConfiguracionInicial.INCOMPLETA
        }
    }

    suspend fun obtenerTaller(): ConfiguracionTaller? =
        database.configuracionTallerDao().obtenerPorId(ID_TALLER)

    suspend fun obtenerAcceso(): ConfiguracionAccesoLocal? =
        database.configuracionAccesoLocalDao().obtenerPorId(ID_ACCESO)

    suspend fun prepararCatalogosSistema(
        monedaDispositivo: String,
        zonaHorariaDispositivo: String,
        monedaConfigurada: String? = null,
        zonaHorariaConfigurada: String? = null
    ): Pair<List<CatalogoMonedaSistema>, List<CatalogoZonaHorariaSistema>> =
        database.withTransaction {
            val monedas = CatalogosSistemaIniciales.monedas(monedaDispositivo, monedaConfigurada)
            val zonasHorarias = CatalogosSistemaIniciales.zonasHorarias(
                zonaHorariaDispositivo,
                zonaHorariaConfigurada
            )
            database.catalogoMonedaSistemaDao().insertarSiNoExisten(monedas)
            database.catalogoZonaHorariaSistemaDao().insertarSiNoExisten(zonasHorarias)
            database.catalogoMonedaSistemaDao().obtenerActivas() to
                database.catalogoZonaHorariaSistemaDao().obtenerActivas()
        }

    suspend fun guardar(
        taller: ConfiguracionTaller,
        acceso: ConfiguracionAccesoLocal
    ) {
        database.withTransaction {
            val tallerDao = database.configuracionTallerDao()
            val accesoDao = database.configuracionAccesoLocalDao()
            val tallerActual = tallerDao.obtenerPorId(ID_TALLER)
            val accesoActual = accesoDao.obtenerPorId(ID_ACCESO)
            check(!(tallerActual != null && accesoActual != null && accesoValido(accesoActual))) {
                "La configuración inicial ya está completa."
            }

            val tallerFinal = taller.copy(
                id = ID_TALLER,
                creadoEn = tallerActual?.creadoEn ?: taller.creadoEn
            )
            if (tallerActual == null) tallerDao.insertar(tallerFinal) else tallerDao.actualizar(tallerFinal)

            val accesoFinal = acceso.copy(
                id = ID_ACCESO,
                actualizadoEn = Instant.now().toString()
            )
            if (accesoActual == null) accesoDao.insertar(accesoFinal) else accesoDao.actualizar(accesoFinal)
        }
    }

    suspend fun autenticarContrasena(contrasena: CharArray): Boolean {
        val acceso = obtenerAcceso() ?: return false
        val verificador = acceso.verificadorContrasena ?: return false
        val sal = acceso.salContrasena ?: return false
        val algoritmo = acceso.algoritmoContrasena ?: return false
        return acceso.metodo == METODO_CONTRASENA &&
            VerificadorContrasena.verificar(contrasena, verificador, sal, algoritmo)
    }

    private fun accesoValido(acceso: ConfiguracionAccesoLocal): Boolean =
        when (acceso.metodo) {
            METODO_CONTRASENA ->
                acceso.verificadorContrasena?.size == VerificadorContrasena.LONGITUD_BITS / 8 &&
                    acceso.salContrasena?.size == VerificadorContrasena.LONGITUD_SAL_BYTES &&
                    acceso.algoritmoContrasena == "${VerificadorContrasena.ALGORITMO}:${VerificadorContrasena.ITERACIONES}:${VerificadorContrasena.LONGITUD_BITS}"
            METODO_CREDENCIAL_ANDROID ->
                acceso.verificadorContrasena == null &&
                    acceso.salContrasena == null &&
                    acceso.algoritmoContrasena == null
            METODO_BIOMETRIA_ANDROID ->
                acceso.verificadorContrasena == null &&
                    acceso.salContrasena == null &&
                    acceso.algoritmoContrasena == null
            else -> false
        }

    companion object {
        const val ID_TALLER = "TALLER"
        const val ID_ACCESO = "ACCESO_LOCAL"
        const val METODO_CONTRASENA = "PASSWORD_APP"
        const val METODO_CREDENCIAL_ANDROID = "CREDENCIAL_ANDROID"
        const val METODO_BIOMETRIA_ANDROID = "BIOMETRIA_ANDROID"
    }
}
