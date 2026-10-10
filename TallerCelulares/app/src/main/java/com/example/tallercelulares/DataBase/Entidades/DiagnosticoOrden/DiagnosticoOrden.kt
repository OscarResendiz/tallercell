package com.example.tallercelulares.DataBase.Entidades.DiagnosticoOrden

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diagnostico_orden",
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
data class DiagnosticoOrden(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    @ColumnInfo(name = "numero_version") val numeroVersion: Int,
    @ColumnInfo(name = "sintomas_reproducidos") val sintomasReproducidos: String,
    val hallazgos: String,
    @ColumnInfo(name = "causa_probable") val causaProbable: String,
    val factibilidad: String,
    val riesgos: String? = null,
    val limitaciones: String? = null,
    @ColumnInfo(name = "tiempo_estimado_minutos") val tiempoEstimadoMinutos: Int? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: String
)
