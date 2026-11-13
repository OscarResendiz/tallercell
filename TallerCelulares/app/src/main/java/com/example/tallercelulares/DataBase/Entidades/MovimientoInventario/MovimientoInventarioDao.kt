package com.example.tallercelulares.DataBase.Entidades.MovimientoInventario

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MovimientoInventarioDao {
    @Query("SELECT * FROM movimiento_inventario WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): MovimientoInventario?

    @Query("SELECT * FROM movimiento_inventario")
    suspend fun obtenerTodos(): List<MovimientoInventario>

    @Query("SELECT * FROM movimiento_inventario WHERE pieza_id = :piezaId")
    suspend fun obtenerPorPiezaId(piezaId: String): List<MovimientoInventario>

    @Query(
        """
        SELECT * FROM movimiento_inventario
        WHERE pieza_id = :piezaId
        ORDER BY COALESCE(confirmada_en, creado_en) DESC, id DESC
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun obtenerPaginaPorPiezaId(
        piezaId: String,
        limite: Int,
        desplazamiento: Int
    ): List<MovimientoInventario>

    @Query("SELECT * FROM movimiento_inventario WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<MovimientoInventario>

    @Query(
        """
        SELECT * FROM movimiento_inventario
        WHERE pieza_id = :piezaId AND tipo = 'RESERVA' AND confirmada_en IS NOT NULL
        ORDER BY confirmada_en DESC, id DESC
        """
    )
    suspend fun obtenerReservasConfirmadas(piezaId: String): List<MovimientoInventario>

    @Query(
        """
        SELECT * FROM movimiento_inventario
        WHERE movimiento_origen_id = :movimientoOrigenId
        ORDER BY creado_en, id
        """
    )
    suspend fun obtenerDerivados(movimientoOrigenId: String): List<MovimientoInventario>

    @Query(
        """
        SELECT * FROM movimiento_inventario
        WHERE pieza_id IN (:piezaIds)
        ORDER BY confirmada_en, creado_en, id
        """
    )
    suspend fun obtenerPorPiezaIds(piezaIds: List<String>): List<MovimientoInventario>

    @Insert
    suspend fun insertar(movimientoInventario: MovimientoInventario)
}
