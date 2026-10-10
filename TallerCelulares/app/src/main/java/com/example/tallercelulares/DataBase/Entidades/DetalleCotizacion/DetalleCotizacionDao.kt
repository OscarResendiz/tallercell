package com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DetalleCotizacionDao {
    @Query("SELECT * FROM detalle_cotizacion WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): DetalleCotizacion?

    @Query("SELECT * FROM detalle_cotizacion")
    suspend fun obtenerTodos(): List<DetalleCotizacion>

    @Query("SELECT * FROM detalle_cotizacion WHERE cotizacion_id = :cotizacionId")
    suspend fun obtenerPorCotizacionId(cotizacionId: String): List<DetalleCotizacion>

    @Query("SELECT * FROM detalle_cotizacion WHERE servicio_id = :servicioId")
    suspend fun obtenerPorServicioId(servicioId: String): List<DetalleCotizacion>

    @Query("SELECT * FROM detalle_cotizacion WHERE pieza_id = :piezaId")
    suspend fun obtenerPorPiezaId(piezaId: String): List<DetalleCotizacion>

    @Insert
    suspend fun insertar(detalleCotizacion: DetalleCotizacion)

    @Update
    suspend fun actualizar(detalleCotizacion: DetalleCotizacion)

    @Delete
    suspend fun eliminar(detalleCotizacion: DetalleCotizacion)
}
