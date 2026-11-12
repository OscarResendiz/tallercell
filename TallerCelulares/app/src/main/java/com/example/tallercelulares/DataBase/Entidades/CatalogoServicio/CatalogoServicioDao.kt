package com.example.tallercelulares.DataBase.Entidades.CatalogoServicio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CatalogoServicioDao {
    @Query("SELECT * FROM catalogo_servicio WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): CatalogoServicio?

    @Query(
        """
        SELECT * FROM catalogo_servicio
        WHERE (:incluirInactivos = 1 OR activo = 1)
          AND (
            :consulta = ''
            OR nombre LIKE '%' || :consulta || '%' ESCAPE '\'
            OR COALESCE(codigo, '') LIKE '%' || :consulta || '%' ESCAPE '\'
          )
        ORDER BY activo DESC, nombre COLLATE NOCASE, codigo COLLATE NOCASE, id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscar(
        consulta: String,
        incluirInactivos: Boolean,
        limite: Int,
        desplazamiento: Int
    ): List<CatalogoServicio>

    @Query(
        """
        SELECT * FROM catalogo_servicio
        WHERE activo = 1
          AND (
            :consulta = ''
            OR nombre LIKE '%' || :consulta || '%' ESCAPE '\'
            OR COALESCE(codigo, '') LIKE '%' || :consulta || '%' ESCAPE '\'
          )
        ORDER BY nombre COLLATE NOCASE, codigo COLLATE NOCASE, id
        LIMIT :limite OFFSET :desplazamiento
        """
    )
    suspend fun buscarActivosParaCotizacion(
        consulta: String,
        limite: Int,
        desplazamiento: Int
    ): List<CatalogoServicio>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM catalogo_servicio
            WHERE codigo = :codigo AND (:excluirId IS NULL OR id != :excluirId)
        )
        """
    )
    suspend fun existeCodigo(codigo: String, excluirId: String?): Boolean

    @Insert
    suspend fun insertar(catalogoServicio: CatalogoServicio)

    @Query(
        """
        UPDATE catalogo_servicio
        SET codigo = :codigo,
            nombre = :nombre,
            descripcion = :descripcion,
            precio_base_minor = :precioBaseMinor,
            actualizado_en = :actualizadoEn
        WHERE id = :id AND activo = 1
        """
    )
    suspend fun actualizarDatos(
        id: String,
        codigo: String?,
        nombre: String,
        descripcion: String,
        precioBaseMinor: Long,
        actualizadoEn: String
    ): Int

    @Query(
        """
        UPDATE catalogo_servicio
        SET activo = 0, actualizado_en = :actualizadoEn
        WHERE id = :id AND activo = 1
        """
    )
    suspend fun desactivar(id: String, actualizadoEn: String): Int
}
