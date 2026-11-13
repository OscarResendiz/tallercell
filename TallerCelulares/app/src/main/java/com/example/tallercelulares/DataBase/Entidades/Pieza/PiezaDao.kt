package com.example.tallercelulares.DataBase.Entidades.Pieza

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PiezaDao {
    @Query("SELECT * FROM pieza WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Pieza?

    @Query(
        """
        SELECT * FROM pieza
        WHERE (:incluirInactivas = 1 OR activo = 1)
          AND (
            :consulta = ''
            OR nombre LIKE '%' || :consulta || '%' ESCAPE '\'
            OR variante LIKE '%' || :consulta || '%' ESCAPE '\'
            OR COALESCE(codigo, '') LIKE '%' || :consulta || '%' ESCAPE '\'
            OR COALESCE(numero_parte, '') LIKE '%' || :consulta || '%' ESCAPE '\'
          )
        ORDER BY activo DESC, nombre COLLATE NOCASE, variante COLLATE NOCASE, id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscar(
        consulta: String,
        incluirInactivas: Boolean,
        limite: Int,
        desplazamiento: Int
    ): List<Pieza>

    @Insert
    suspend fun insertar(pieza: Pieza)

    @Query(
        """
        UPDATE pieza
        SET codigo = :codigo,
            numero_parte = :numeroParte,
            nombre = :nombre,
            variante = :variante,
            calidad = :calidad,
            costo_unitario_minor = :costoUnitarioMinor,
            precio_venta_minor = :precioVentaMinor,
            existencia_minima = :existenciaMinima,
            actualizado_en = :actualizadoEn
        WHERE id = :id AND activo = 1
        """
    )
    suspend fun actualizarDatos(
        id: String,
        codigo: String?,
        numeroParte: String?,
        nombre: String,
        variante: String,
        calidad: String,
        costoUnitarioMinor: Long,
        precioVentaMinor: Long,
        existenciaMinima: Int,
        actualizadoEn: String
    ): Int

    @Query(
        """
        UPDATE pieza
        SET activo = 0, actualizado_en = :actualizadoEn
        WHERE id = :id AND activo = 1
        """
    )
    suspend fun desactivar(id: String, actualizadoEn: String): Int
}
