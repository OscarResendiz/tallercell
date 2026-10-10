package com.example.tallercelulares.DataBase.Entidades.Pieza

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pieza")
data class Pieza(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    val codigo: String? = null,
    @ColumnInfo(name = "numero_parte") val numeroParte: String? = null,
    val nombre: String,
    val variante: String,
    val calidad: String,
    @ColumnInfo(name = "costo_unitario_minor") val costoUnitarioMinor: Long,
    @ColumnInfo(name = "precio_venta_minor") val precioVentaMinor: Long,
    @ColumnInfo(name = "existencia_minima") val existenciaMinima: Int,
    val activo: Boolean,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
