package com.example.tallercelulares.DataBase.Entidades.OrdenServicio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery

@Dao
interface OrdenServicioDao {
    @Query("SELECT * FROM orden_servicio WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): OrdenServicio?

    @Query("SELECT * FROM orden_servicio")
    suspend fun obtenerTodos(): List<OrdenServicio>

    @Query("SELECT * FROM orden_servicio WHERE cliente_id = :clienteId ORDER BY recibida_en DESC, folio, id")
    suspend fun obtenerPorClienteId(clienteId: String): List<OrdenServicio>

    @Query(
        """
        SELECT * FROM orden_servicio
        WHERE eliminada_en IS NULL
          AND (
            :consulta = ''
            OR folio LIKE '%' || :consulta || '%' ESCAPE '\'
            OR cliente_nombre_snapshot LIKE '%' || :consulta || '%' ESCAPE '\'
            OR marca_snapshot LIKE '%' || :consulta || '%' ESCAPE '\'
            OR modelo_snapshot LIKE '%' || :consulta || '%' ESCAPE '\'
          )
        ORDER BY recibida_en DESC, folio, id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscarParaCotizacion(
        consulta: String,
        limite: Int,
        desplazamiento: Int
    ): List<OrdenServicio>

    @Query(
        """
        SELECT * FROM orden_servicio
        WHERE dispositivo_id = :dispositivoId
        ORDER BY recibida_en DESC, folio, id
        """
    )
    suspend fun obtenerPorDispositivoId(dispositivoId: String): List<OrdenServicio>

    @Query("SELECT * FROM orden_servicio WHERE orden_origen_id = :ordenOrigenId")
    suspend fun obtenerPorOrdenOrigenId(ordenOrigenId: String): List<OrdenServicio>

    @RawQuery
    suspend fun consultarTablero(query: SupportSQLiteQuery): List<OrdenTableroRegistro>

    @Query(
        """
        SELECT estado, COUNT(id) AS cantidad
        FROM orden_servicio
        WHERE eliminada_en IS NULL AND estado NOT IN ('ENTREGADA', 'CANCELADA')
        GROUP BY estado
        """
    )
    suspend fun obtenerConteosTablero(): List<ConteoOrdenTablero>

    @Query("SELECT COUNT(id) FROM orden_servicio WHERE eliminada_en IS NULL")
    suspend fun obtenerCantidadOrdenesRegistradas(): Int

    @Insert
    suspend fun insertar(ordenServicio: OrdenServicio)

    @Update
    suspend fun actualizar(ordenServicio: OrdenServicio)

    @Delete
    suspend fun eliminar(ordenServicio: OrdenServicio)
}
