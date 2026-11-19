package com.example.tallercelulares.DataBase.Entidades.OpcionMetodoPago

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OpcionMetodoPagoDao {
    @Query("SELECT * FROM opcion_metodo_pago WHERE activo = 1 ORDER BY nombre COLLATE NOCASE, id")
    suspend fun obtenerActivas(): List<OpcionMetodoPago>

    @Query("SELECT * FROM opcion_metodo_pago ORDER BY activo DESC, nombre COLLATE NOCASE, id")
    suspend fun obtenerTodas(): List<OpcionMetodoPago>

    @Query("SELECT * FROM opcion_metodo_pago WHERE nombre = :nombre COLLATE NOCASE LIMIT 1")
    suspend fun obtenerPorNombre(nombre: String): OpcionMetodoPago?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(opcion: OpcionMetodoPago)

    @Query("UPDATE opcion_metodo_pago SET activo = :activo, actualizado_en = :actualizadoEn WHERE id = :id")
    suspend fun actualizarEstado(id: String, activo: Boolean, actualizadoEn: String): Int
}
