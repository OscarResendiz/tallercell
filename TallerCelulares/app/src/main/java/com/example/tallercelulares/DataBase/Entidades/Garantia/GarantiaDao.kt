package com.example.tallercelulares.DataBase.Entidades.Garantia

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GarantiaDao {
    @Query("SELECT * FROM garantia WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Garantia?

    @Query("SELECT * FROM garantia")
    suspend fun obtenerTodos(): List<Garantia>

    @Query("SELECT * FROM garantia WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<Garantia>

    @Insert
    suspend fun insertar(garantia: Garantia)

    @Update
    suspend fun actualizar(garantia: Garantia)

    @Delete
    suspend fun eliminar(garantia: Garantia)
}
