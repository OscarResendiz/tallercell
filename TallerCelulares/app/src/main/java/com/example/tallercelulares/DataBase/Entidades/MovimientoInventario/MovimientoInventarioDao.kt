package com.example.tallercelulares.DataBase.Entidades.MovimientoInventario

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MovimientoInventarioDao {
    @Query("SELECT * FROM movimiento_inventario WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): MovimientoInventario?

    @Query("SELECT * FROM movimiento_inventario")
    suspend fun obtenerTodos(): List<MovimientoInventario>

    @Query("SELECT * FROM movimiento_inventario WHERE pieza_id = :piezaId")
    suspend fun obtenerPorPiezaId(piezaId: String): List<MovimientoInventario>

    @Query("SELECT * FROM movimiento_inventario WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<MovimientoInventario>

    @Insert
    suspend fun insertar(movimientoInventario: MovimientoInventario)

    @Update
    suspend fun actualizar(movimientoInventario: MovimientoInventario)

    @Delete
    suspend fun eliminar(movimientoInventario: MovimientoInventario)
}
