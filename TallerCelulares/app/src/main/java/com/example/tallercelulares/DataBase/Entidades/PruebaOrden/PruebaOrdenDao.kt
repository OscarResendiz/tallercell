package com.example.tallercelulares.DataBase.Entidades.PruebaOrden

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PruebaOrdenDao {
    @Query("SELECT * FROM prueba_orden WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): PruebaOrden?

    @Query("SELECT * FROM prueba_orden")
    suspend fun obtenerTodos(): List<PruebaOrden>

    @Query("SELECT * FROM prueba_orden WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<PruebaOrden>

    @Query("SELECT * FROM prueba_orden WHERE prueba_ingreso_id = :pruebaIngresoId")
    suspend fun obtenerPorPruebaIngresoId(pruebaIngresoId: String): List<PruebaOrden>

    @Insert
    suspend fun insertar(pruebaOrden: PruebaOrden)

    @Update
    suspend fun actualizar(pruebaOrden: PruebaOrden)

    @Delete
    suspend fun eliminar(pruebaOrden: PruebaOrden)
}
