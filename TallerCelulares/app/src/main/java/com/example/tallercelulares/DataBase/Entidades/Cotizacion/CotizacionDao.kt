package com.example.tallercelulares.DataBase.Entidades.Cotizacion

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CotizacionDao {
    @Query("SELECT * FROM cotizacion WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Cotizacion?

    @Query("SELECT * FROM cotizacion")
    suspend fun obtenerTodos(): List<Cotizacion>

    @Query("SELECT * FROM cotizacion WHERE orden_id = :ordenId")
    suspend fun obtenerPorOrdenId(ordenId: String): List<Cotizacion>

    @Query(
        """
        SELECT * FROM cotizacion
        WHERE orden_id = :ordenId AND estado = 'BORRADOR'
        ORDER BY numero_version DESC
        LIMIT 1
        """
    )
    suspend fun obtenerBorradorMasReciente(ordenId: String): Cotizacion?

    @Query("SELECT MAX(numero_version) FROM cotizacion WHERE orden_id = :ordenId")
    suspend fun obtenerMaximaVersion(ordenId: String): Int?

    @Query(
        """
        UPDATE cotizacion
        SET subtotal_minor = :subtotalMinor,
            impuesto_minor = :impuestoMinor,
            total_minor = :totalMinor
        WHERE id = :id AND estado = 'BORRADOR'
        """
    )
    suspend fun actualizarTotalesBorrador(
        id: String,
        subtotalMinor: Long,
        impuestoMinor: Long,
        totalMinor: Long
    ): Int

    @Insert
    suspend fun insertar(cotizacion: Cotizacion)

    @Update
    suspend fun actualizar(cotizacion: Cotizacion)

    @Delete
    suspend fun eliminar(cotizacion: Cotizacion)
}
