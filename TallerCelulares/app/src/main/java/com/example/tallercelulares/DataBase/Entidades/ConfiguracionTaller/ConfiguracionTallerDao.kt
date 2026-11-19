package com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConfiguracionTallerDao {
    @Query("SELECT * FROM configuracion_taller WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): ConfiguracionTaller?

    @Query("SELECT * FROM configuracion_taller")
    suspend fun obtenerTodos(): List<ConfiguracionTaller>

    @Insert
    suspend fun insertar(configuracionTaller: ConfiguracionTaller)

    @Update
    suspend fun actualizar(configuracionTaller: ConfiguracionTaller)

    @Query("""
        UPDATE configuracion_taller SET
        nombre = :nombre,
        telefono = :telefono,
        direccion = :direccion,
        moneda_codigo = :monedaCodigo,
        zona_horaria = :zonaHoraria,
        prefijo_folio = :prefijoFolio,
        siguiente_folio = :siguienteFolio,
        impuesto_tasa = :impuestoTasa,
        cargo_diagnostico_minor = :cargoDiagnosticoMinor,
        dias_vigencia_cotizacion = :diasVigenciaCotizacion,
        dias_garantia_predeterminados = :diasGarantiaPredeterminados,
        dias_recordatorio_seguimiento_autorizacion = :diasSeguimientoAutorizacion,
        dias_recordatorio_refaccion_atrasada = :diasRefaccionAtrasada,
        dias_recordatorio_equipo_listo = :diasEquipoListo,
        texto_recepcion = :textoRecepcion,
        texto_autorizacion = :textoAutorizacion,
        texto_garantia = :textoGarantia,
        actualizado_en = :actualizadoEn
        WHERE id = :id
    """)
    suspend fun actualizarCampos(
        id: String,
        nombre: String?,
        telefono: String?,
        direccion: String?,
        monedaCodigo: String,
        zonaHoraria: String,
        prefijoFolio: String,
        siguienteFolio: Long,
        impuestoTasa: String,
        cargoDiagnosticoMinor: Long,
        diasVigenciaCotizacion: Int,
        diasGarantiaPredeterminados: Int,
        diasSeguimientoAutorizacion: Int,
        diasRefaccionAtrasada: Int,
        diasEquipoListo: Int,
        textoRecepcion: String?,
        textoAutorizacion: String?,
        textoGarantia: String?,
        actualizadoEn: String
    ): Int

    @Delete
    suspend fun eliminar(configuracionTaller: ConfiguracionTaller)
}
