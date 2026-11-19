package com.example.tallercelulares.DataBase.Entidades.OpcionMetodoPago

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.tallercelulares.DataBase.Entidades.EntidadId

@Entity(
    tableName = "opcion_metodo_pago",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class OpcionMetodoPago(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    val nombre: String,
    val activo: Boolean = true,
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
