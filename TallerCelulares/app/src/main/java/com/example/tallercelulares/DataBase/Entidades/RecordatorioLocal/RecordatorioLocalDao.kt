package com.example.tallercelulares.DataBase.Entidades.RecordatorioLocal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RecordatorioLocalDao {
    @Query("SELECT * FROM recordatorio_local WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): RecordatorioLocal?

    @Query("SELECT * FROM recordatorio_local")
    suspend fun obtenerTodos(): List<RecordatorioLocal>

    @Query("SELECT * FROM recordatorio_local WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<RecordatorioLocal>

    @Insert
    suspend fun insertar(recordatorioLocal: RecordatorioLocal)

    @Update
    suspend fun actualizar(recordatorioLocal: RecordatorioLocal)

    @Delete
    suspend fun eliminar(recordatorioLocal: RecordatorioLocal)
}
