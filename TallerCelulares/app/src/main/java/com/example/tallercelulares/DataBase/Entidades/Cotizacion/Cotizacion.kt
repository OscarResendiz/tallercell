package com.example.tallercelulares.DataBase.Entidades.Cotizacion

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cotizacion",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["orden_id"]),
        Index(value = ["orden_id", "numero_version"], unique = true)
    ]
)
data class Cotizacion(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    @ColumnInfo(name = "numero_version") val numeroVersion: Int,
    @ColumnInfo(name = "subtotal_minor") val subtotalMinor: Long,
    @ColumnInfo(name = "descuento_minor") val descuentoMinor: Long,
    @ColumnInfo(name = "impuesto_minor") val impuestoMinor: Long,
    @ColumnInfo(name = "total_minor") val totalMinor: Long,
    @ColumnInfo(name = "moneda_codigo") val monedaCodigo: String,
    @ColumnInfo(name = "vigente_hasta") val vigenteHasta: String? = null,
    @ColumnInfo(name = "tiempo_estimado_minutos") val tiempoEstimadoMinutos: Int? = null,
    @ColumnInfo(name = "riesgos_limitaciones") val riesgosLimitaciones: String? = null,
    val condiciones: String? = null,
    val estado: String,
    @ColumnInfo(name = "creada_en") val creadaEn: String
)
