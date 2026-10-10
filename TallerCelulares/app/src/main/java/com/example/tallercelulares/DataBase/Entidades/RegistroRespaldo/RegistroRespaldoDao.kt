package com.example.tallercelulares.DataBase.Entidades.RegistroRespaldo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RegistroRespaldoDao {
    @Query("SELECT * FROM registro_respaldo WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): RegistroRespaldo?

    @Query("SELECT * FROM registro_respaldo")
    suspend fun obtenerTodos(): List<RegistroRespaldo>

    @Insert
    suspend fun insertar(registroRespaldo: RegistroRespaldo)

    @Update
    suspend fun actualizar(registroRespaldo: RegistroRespaldo)

    @Delete
    suspend fun eliminar(registroRespaldo: RegistroRespaldo)
}
