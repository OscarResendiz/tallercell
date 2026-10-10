package com.example.tallercelulares.DataBase.Entidades.AccesorioOrden

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AccesorioOrdenDao {
    @Query("SELECT * FROM accesorio_orden WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): AccesorioOrden?

    @Query("SELECT * FROM accesorio_orden")
    suspend fun obtenerTodos(): List<AccesorioOrden>

    @Query("SELECT * FROM accesorio_orden WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<AccesorioOrden>

    @Insert
    suspend fun insertar(accesorioOrden: AccesorioOrden)

    @Update
    suspend fun actualizar(accesorioOrden: AccesorioOrden)

    @Delete
    suspend fun eliminar(accesorioOrden: AccesorioOrden)
}
