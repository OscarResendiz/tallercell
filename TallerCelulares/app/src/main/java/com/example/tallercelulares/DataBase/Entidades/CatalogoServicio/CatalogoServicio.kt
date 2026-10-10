package com.example.tallercelulares.DataBase.Entidades.CatalogoServicio

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalogo_servicio",
    indices = [Index(value = ["codigo"], unique = true)]
)
data class CatalogoServicio(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    val codigo: String? = null,
    val nombre: String,
    val descripcion: String,
    @ColumnInfo(name = "precio_base_minor") val precioBaseMinor: Long,
    val activo: Boolean,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
