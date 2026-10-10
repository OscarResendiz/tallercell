package com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AutorizacionCotizacionDao {
    @Query("SELECT * FROM autorizacion_cotizacion WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AutorizacionCotizacion?

    @Query("SELECT * FROM autorizacion_cotizacion")
    suspend fun obtenerTodos(): List<AutorizacionCotizacion>

    @Query("SELECT * FROM autorizacion_cotizacion WHERE cotizacion_id = :cotizacionId")
    suspend fun obtenerPorCotizacionId(cotizacionId: String): List<AutorizacionCotizacion>

    @Query("SELECT * FROM autorizacion_cotizacion WHERE evidencia_adjunto_id = :adjuntoId")
    suspend fun obtenerPorEvidenciaAdjuntoId(adjuntoId: String): List<AutorizacionCotizacion>

    @Insert
    suspend fun insertar(autorizacionCotizacion: AutorizacionCotizacion)

    @Update
    suspend fun actualizar(autorizacionCotizacion: AutorizacionCotizacion)

    @Delete
    suspend fun eliminar(autorizacionCotizacion: AutorizacionCotizacion)
}
