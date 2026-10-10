package com.example.tallercelulares.DataBase.Entidades.MovimientoCaja

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MovimientoCajaDao {
    @Query("SELECT * FROM movimiento_caja WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): MovimientoCaja?

    @Query("SELECT * FROM movimiento_caja")
    suspend fun obtenerTodos(): List<MovimientoCaja>

    @Query("SELECT * FROM movimiento_caja WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<MovimientoCaja>

    @Query("SELECT * FROM movimiento_caja WHERE movimiento_origen_id = :movimientoOrigenId")
    suspend fun obtenerPorMovimientoOrigenId(movimientoOrigenId: String): List<MovimientoCaja>

    @Insert
    suspend fun insertar(movimientoCaja: MovimientoCaja)

    @Update
    suspend fun actualizar(movimientoCaja: MovimientoCaja)

    @Delete
    suspend fun eliminar(movimientoCaja: MovimientoCaja)
}
