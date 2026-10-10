package com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConfiguracionAccesoLocalDao {
    @Query("SELECT * FROM configuracion_acceso_local WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ConfiguracionAccesoLocal?

    @Query("SELECT * FROM configuracion_acceso_local")
    suspend fun obtenerTodos(): List<ConfiguracionAccesoLocal>

    @Insert
    suspend fun insertar(configuracionAccesoLocal: ConfiguracionAccesoLocal)

    @Update
    suspend fun actualizar(configuracionAccesoLocal: ConfiguracionAccesoLocal)

    @Delete
    suspend fun eliminar(configuracionAccesoLocal: ConfiguracionAccesoLocal)
}
