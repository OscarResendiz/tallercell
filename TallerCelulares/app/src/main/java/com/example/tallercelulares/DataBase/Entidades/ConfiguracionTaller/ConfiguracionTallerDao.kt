package com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConfiguracionTallerDao {
    @Query("SELECT * FROM configuracion_taller WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ConfiguracionTaller?

    @Query("SELECT * FROM configuracion_taller")
    suspend fun obtenerTodos(): List<ConfiguracionTaller>

    @Insert
    suspend fun insertar(configuracionTaller: ConfiguracionTaller)

    @Update
    suspend fun actualizar(configuracionTaller: ConfiguracionTaller)

    @Delete
    suspend fun eliminar(configuracionTaller: ConfiguracionTaller)
}
