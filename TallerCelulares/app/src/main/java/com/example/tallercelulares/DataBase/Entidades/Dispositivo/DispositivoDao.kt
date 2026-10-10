package com.example.tallercelulares.DataBase.Entidades.Dispositivo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DispositivoDao {
    @Query("SELECT * FROM dispositivo WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Dispositivo?

    @Query("SELECT * FROM dispositivo")
    suspend fun obtenerTodos(): List<Dispositivo>

    @Query("SELECT * FROM dispositivo WHERE cliente_id = :clienteId")
    suspend fun obtenerPorClienteId(clienteId: String): List<Dispositivo>

    @Insert
    suspend fun insertar(dispositivo: Dispositivo)

    @Update
    suspend fun actualizar(dispositivo: Dispositivo)

    @Delete
    suspend fun eliminar(dispositivo: Dispositivo)
}
