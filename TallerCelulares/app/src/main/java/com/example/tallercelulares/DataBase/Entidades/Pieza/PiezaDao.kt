package com.example.tallercelulares.DataBase.Entidades.Pieza

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PiezaDao {
    @Query("SELECT * FROM pieza WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Pieza?

    @Query("SELECT * FROM pieza")
    suspend fun obtenerTodos(): List<Pieza>

    @Insert
    suspend fun insertar(pieza: Pieza)

    @Update
    suspend fun actualizar(pieza: Pieza)

    @Delete
    suspend fun eliminar(pieza: Pieza)
}
