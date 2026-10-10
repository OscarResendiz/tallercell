package com.example.tallercelulares.DataBase.Entidades.DiagnosticoOrden

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface DiagnosticoOrdenDao {
    @Query("SELECT * FROM diagnostico_orden WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): DiagnosticoOrden?

    @Query("SELECT * FROM diagnostico_orden")
    suspend fun obtenerTodos(): List<DiagnosticoOrden>

    @Query("SELECT * FROM diagnostico_orden WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<DiagnosticoOrden>

    @Insert
    suspend fun insertar(diagnosticoOrden: DiagnosticoOrden)

    @Update
    suspend fun actualizar(diagnosticoOrden: DiagnosticoOrden)

    @Delete
    suspend fun eliminar(diagnosticoOrden: DiagnosticoOrden)
}
