package com.example.tallercelulares.DataBase.Entidades.EventoOrden

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventoOrdenDao {
    @Query("SELECT * FROM evento_orden WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): EventoOrden?

    @Query("SELECT * FROM evento_orden")
    suspend fun obtenerTodos(): List<EventoOrden>

    @Query("SELECT * FROM evento_orden WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<EventoOrden>

    @Insert
    suspend fun insertar(eventoOrden: EventoOrden)

    @Update
    suspend fun actualizar(eventoOrden: EventoOrden)

    @Delete
    suspend fun eliminar(eventoOrden: EventoOrden)
}
