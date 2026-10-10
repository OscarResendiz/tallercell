package com.example.tallercelulares.DataBase.Entidades.Cliente

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClienteDao {
    @Query("SELECT * FROM cliente WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Cliente?

    @Query("SELECT * FROM cliente")
    suspend fun obtenerTodos(): List<Cliente>

    @Insert
    suspend fun insertar(cliente: Cliente)

    @Update
    suspend fun actualizar(cliente: Cliente)

    @Delete
    suspend fun eliminar(cliente: Cliente)
}
