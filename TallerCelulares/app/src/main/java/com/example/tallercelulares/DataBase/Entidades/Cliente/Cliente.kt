package com.example.tallercelulares.DataBase.Entidades.Cliente

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cliente",
    indices = [Index(value = ["nombre"]), Index(value = ["telefono"])]
)
data class Cliente(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    val nombre: String,
    val telefono: String? = null,
    val correo: String? = null,
    val notas: String? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String,
    @ColumnInfo(name = "eliminado_en") val eliminadoEn: String? = null
)
