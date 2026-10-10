package com.example.tallercelulares.DataBase.Entidades.CredencialEquipo

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CredencialEquipoDao {
    @Query("SELECT * FROM credencial_equipo WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): CredencialEquipo?

    @Query("SELECT * FROM credencial_equipo")
    suspend fun obtenerTodos(): List<CredencialEquipo>

    @Query("SELECT * FROM credencial_equipo WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<CredencialEquipo>

    @Insert
    suspend fun insertar(credencialEquipo: CredencialEquipo)

    @Update
    suspend fun actualizar(credencialEquipo: CredencialEquipo)

    @Delete
    suspend fun eliminar(credencialEquipo: CredencialEquipo)
}
