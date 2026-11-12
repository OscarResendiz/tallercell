package com.example.tallercelulares.DataBase.Entidades.Dispositivo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DispositivoDao {
    @Query("SELECT * FROM dispositivo WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Dispositivo?

    @Query("SELECT * FROM dispositivo WHERE id = :id AND eliminado_en IS NULL LIMIT 1")
    suspend fun obtenerActivoPorId(id: String): Dispositivo?

    @Query(
        """
        SELECT d.* FROM dispositivo d
        INNER JOIN cliente c ON c.id = d.cliente_id
        WHERE d.cliente_id = :clienteId
          AND d.eliminado_en IS NULL
          AND c.eliminado_en IS NULL
          AND (
            :consulta = ''
            OR d.tipo LIKE '%' || :consulta || '%' ESCAPE '\'
            OR d.marca LIKE '%' || :consulta || '%' ESCAPE '\'
            OR d.modelo LIKE '%' || :consulta || '%' ESCAPE '\'
            OR d.color LIKE '%' || :consulta || '%' ESCAPE '\'
          )
        ORDER BY d.marca COLLATE NOCASE, d.modelo COLLATE NOCASE, d.tipo COLLATE NOCASE, d.id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscarActivosPorCliente(
        clienteId: String,
        consulta: String,
        limite: Int,
        desplazamiento: Int
    ): List<Dispositivo>

    @Query(
        """
        SELECT * FROM dispositivo
        WHERE cliente_id = :clienteId
        ORDER BY eliminado_en IS NOT NULL, marca COLLATE NOCASE, modelo COLLATE NOCASE, tipo COLLATE NOCASE, id
        """
    )
    suspend fun obtenerPorClienteId(clienteId: String): List<Dispositivo>

    @Query(
        """
        SELECT * FROM dispositivo
        WHERE cliente_id = :clienteId
          AND eliminado_en IS NULL
          AND (
            (:imei IS NOT NULL AND imei = :imei)
            OR (:numeroSerie IS NOT NULL AND numero_serie = :numeroSerie)
          )
        ORDER BY marca COLLATE NOCASE, modelo COLLATE NOCASE, id
        LIMIT :limite
        """
    )
    suspend fun buscarCoincidenciasActivas(
        clienteId: String,
        imei: String?,
        numeroSerie: String?,
        limite: Int
    ): List<Dispositivo>

    @Insert
    suspend fun insertar(dispositivo: Dispositivo)

    @Query(
        """
        UPDATE dispositivo
        SET tipo = :tipo,
            marca = :marca,
            modelo = :modelo,
            color = :color,
            numero_serie = :numeroSerie,
            imei = :imei,
            actualizado_en = :actualizadoEn
        WHERE id = :id
          AND cliente_id = :clienteId
          AND eliminado_en IS NULL
        """
    )
    suspend fun actualizarDatos(
        id: String,
        clienteId: String,
        tipo: String,
        marca: String,
        modelo: String,
        color: String,
        numeroSerie: String?,
        imei: String?,
        actualizadoEn: String
    ): Int

    @Query(
        """
        UPDATE dispositivo
        SET eliminado_en = :eliminadoEn, actualizado_en = :actualizadoEn
        WHERE id = :id AND eliminado_en IS NULL
        """
    )
    suspend fun darDeBaja(id: String, eliminadoEn: String, actualizadoEn: String): Int
}
