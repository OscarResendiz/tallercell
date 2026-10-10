package com.example.tallercelulares.DataBase.Entidades.Dispositivo

import com.example.tallercelulares.DataBase.Entidades.Cliente.Cliente
import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dispositivo",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["cliente_id"]),
        Index(value = ["imei"]),
        Index(value = ["numero_serie"])
    ]
)
data class Dispositivo(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "cliente_id") val clienteId: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val color: String,
    @ColumnInfo(name = "numero_serie") val numeroSerie: String? = null,
    val imei: String? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
