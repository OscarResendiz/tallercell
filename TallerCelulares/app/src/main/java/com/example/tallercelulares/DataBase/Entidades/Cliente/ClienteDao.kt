package com.example.tallercelulares.DataBase.Entidades.Cliente

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClienteDao {
    @Query("SELECT * FROM cliente WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): Cliente?

    @Query("SELECT * FROM cliente WHERE id = :id AND eliminado_en IS NULL LIMIT 1")
    suspend fun obtenerActivoPorId(id: String): Cliente?

    @Query(
        """
        SELECT * FROM cliente
        WHERE eliminado_en IS NULL
          AND (
            :consulta = ''
            OR $NOMBRE_BUSQUEDA_NORMALIZADO LIKE '%' || :consulta || '%' ESCAPE '\'
            OR (
                length(:telefono) >= 3
                AND $TELEFONO_NORMALIZADO LIKE '%' || :telefono || '%'
            )
          )
        ORDER BY nombre COLLATE NOCASE, id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscarActivos(
        consulta: String,
        telefono: String,
        limite: Int,
        desplazamiento: Int
    ): List<Cliente>

    @Query(
        """
        SELECT * FROM cliente
        WHERE eliminado_en IS NULL
          AND (
            $NOMBRE_BUSQUEDA_NORMALIZADO LIKE '%' || :nombre || '%' ESCAPE '\'
            OR (
                length(:telefono) >= 3
                AND $TELEFONO_NORMALIZADO LIKE '%' || :telefono || '%'
            )
          )
        ORDER BY nombre COLLATE NOCASE, id
        LIMIT :limite
        """
    )
    suspend fun buscarCoincidenciasActivas(
        nombre: String,
        telefono: String,
        limite: Int
    ): List<Cliente>

    @Insert
    suspend fun insertar(cliente: Cliente)

    @Query(
        """
        UPDATE cliente
        SET nombre = :nombre,
            telefono = :telefono,
            correo = :correo,
            notas = :notas,
            actualizado_en = :actualizadoEn
        WHERE id = :id AND eliminado_en IS NULL
        """
    )
    suspend fun actualizarDatos(
        id: String,
        nombre: String,
        telefono: String?,
        correo: String?,
        notas: String?,
        actualizadoEn: String
    ): Int

    @Query(
        """
        UPDATE cliente
        SET eliminado_en = :eliminadoEn, actualizado_en = :actualizadoEn
        WHERE id = :id AND eliminado_en IS NULL
        """
    )
    suspend fun darDeBaja(id: String, eliminadoEn: String, actualizadoEn: String): Int

    companion object {
        private const val NOMBRE_NORMALIZADO =
            "LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(nombre, 'á', 'a'), 'Á', 'a'), 'é', 'e'), 'É', 'e'), 'í', 'i'), 'Í', 'i'), 'ó', 'o'), 'Ó', 'o'), 'ú', 'u'), 'Ú', 'u'), 'ü', 'u'), 'Ü', 'u'), 'ñ', 'n'), 'Ñ', 'n'))"
        private const val NOMBRE_BUSQUEDA_NORMALIZADO =
            "REPLACE(REPLACE(REPLACE(TRIM($NOMBRE_NORMALIZADO), '  ', ' '), '  ', ' '), '  ', ' ')"
        private const val TELEFONO_NORMALIZADO =
            "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(telefono, ' ', ''), '+', ''), '-', ''), '(', ''), ')', ''), '.', ''), '/', ''), '\\', '')"
    }
}
