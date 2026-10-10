package com.example.tallercelulares.DataBase.Entidades.OrdenServicio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface OrdenServicioDao {
    @Query("SELECT * FROM orden_servicio WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): OrdenServicio?

    @Query("SELECT * FROM orden_servicio")
    suspend fun obtenerTodos(): List<OrdenServicio>

    @Query("SELECT * FROM orden_servicio WHERE cliente_id = :clienteId")
    suspend fun obtenerPorClienteId(clienteId: String): List<OrdenServicio>

    @Query("SELECT * FROM orden_servicio WHERE dispositivo_id = :dispositivoId")
    suspend fun obtenerPorDispositivoId(dispositivoId: String): List<OrdenServicio>

    @Query("SELECT * FROM orden_servicio WHERE orden_origen_id = :ordenOrigenId")
    suspend fun obtenerPorOrdenOrigenId(ordenOrigenId: String): List<OrdenServicio>

    @Insert
    suspend fun insertar(ordenServicio: OrdenServicio)

    @Update
    suspend fun actualizar(ordenServicio: OrdenServicio)

    @Delete
    suspend fun eliminar(ordenServicio: OrdenServicio)
}
