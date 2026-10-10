package com.example.tallercelulares.DataBase.Entidades.TrabajoRealizado

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface TrabajoRealizadoDao {
    @Query("SELECT * FROM trabajo_realizado WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): TrabajoRealizado?

    @Query("SELECT * FROM trabajo_realizado")
    suspend fun obtenerTodos(): List<TrabajoRealizado>

    @Query("SELECT * FROM trabajo_realizado WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<TrabajoRealizado>

    @Insert
    suspend fun insertar(trabajoRealizado: TrabajoRealizado)

    @Update
    suspend fun actualizar(trabajoRealizado: TrabajoRealizado)

    @Delete
    suspend fun eliminar(trabajoRealizado: TrabajoRealizado)
}
