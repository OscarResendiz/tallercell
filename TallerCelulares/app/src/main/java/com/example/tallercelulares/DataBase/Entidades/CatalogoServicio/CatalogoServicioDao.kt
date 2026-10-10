package com.example.tallercelulares.DataBase.Entidades.CatalogoServicio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CatalogoServicioDao {
    @Query("SELECT * FROM catalogo_servicio WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): CatalogoServicio?

    @Query("SELECT * FROM catalogo_servicio")
    suspend fun obtenerTodos(): List<CatalogoServicio>

    @Insert
    suspend fun insertar(catalogoServicio: CatalogoServicio)

    @Update
    suspend fun actualizar(catalogoServicio: CatalogoServicio)

    @Delete
    suspend fun eliminar(catalogoServicio: CatalogoServicio)
}
