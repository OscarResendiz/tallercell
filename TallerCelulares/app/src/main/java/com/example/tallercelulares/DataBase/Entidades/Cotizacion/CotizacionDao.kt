package com.example.tallercelulares.DataBase.Entidades.Cotizacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CotizacionDao {
    @Query("SELECT * FROM cotizacion WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Cotizacion?

    @Query("SELECT * FROM cotizacion")
    suspend fun obtenerTodos(): List<Cotizacion>

    @Query("SELECT * FROM cotizacion WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<Cotizacion>

    @Insert
    suspend fun insertar(cotizacion: Cotizacion)

    @Update
    suspend fun actualizar(cotizacion: Cotizacion)

    @Delete
    suspend fun eliminar(cotizacion: Cotizacion)
}
