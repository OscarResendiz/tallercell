package com.example.tallercelulares.DataBase.Entidades.PruebaOrden

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prueba_orden",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PruebaOrden::class,
            parentColumns = ["id"],
            childColumns = ["prueba_ingreso_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["orden_id"]), Index(value = ["prueba_ingreso_id"])]
)
data class PruebaOrden(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val etapa: String,
    @ColumnInfo(name = "nombre_prueba") val nombrePrueba: String,
    val resultado: String,
    val observacion: String? = null,
    @ColumnInfo(name = "motivo_no_realizada") val motivoNoRealizada: String? = null,
    @ColumnInfo(name = "prueba_ingreso_id") val pruebaIngresoId: String? = null,
    @ColumnInfo(name = "registrada_en") val registradaEn: String
)
