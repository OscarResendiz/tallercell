package com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion_taller")
data class ConfiguracionTaller(
    @PrimaryKey val id: String = "TALLER",
    val nombre: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    @ColumnInfo(name = "moneda_codigo") val monedaCodigo: String,
    @ColumnInfo(name = "zona_horaria") val zonaHoraria: String,
    @ColumnInfo(name = "prefijo_folio") val prefijoFolio: String,
    @ColumnInfo(name = "siguiente_folio") val siguienteFolio: Long,
    @ColumnInfo(name = "impuesto_tasa") val impuestoTasa: String,
    @ColumnInfo(name = "cargo_diagnostico_minor") val cargoDiagnosticoMinor: Long,
    @ColumnInfo(name = "dias_vigencia_cotizacion") val diasVigenciaCotizacion: Int,
    @ColumnInfo(name = "dias_garantia_predeterminados") val diasGarantiaPredeterminados: Int,
    @ColumnInfo(name = "dias_recordatorio_seguimiento_autorizacion")
    val diasRecordatorioSeguimientoAutorizacion: Int,
    @ColumnInfo(name = "dias_recordatorio_refaccion_atrasada")
    val diasRecordatorioRefaccionAtrasada: Int,
    @ColumnInfo(name = "dias_recordatorio_equipo_listo")
    val diasRecordatorioEquipoListo: Int,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
