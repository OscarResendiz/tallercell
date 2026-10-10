package com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AdjuntoOrdenDao {
    @Query("SELECT * FROM adjunto_orden WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AdjuntoOrden?

    @Query("SELECT * FROM adjunto_orden")
    suspend fun obtenerTodos(): List<AdjuntoOrden>

    @Query("SELECT * FROM adjunto_orden WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<AdjuntoOrden>

    @Insert
    suspend fun insertar(adjuntoOrden: AdjuntoOrden)

    @Update
    suspend fun actualizar(adjuntoOrden: AdjuntoOrden)

    @Delete
    suspend fun eliminar(adjuntoOrden: AdjuntoOrden)
}
