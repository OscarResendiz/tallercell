package com.example.tallercelulares.DataBase.Entidades.EventoOrden

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evento_orden",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["orden_id", "registrado_en"])]
)
data class EventoOrden(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    @ColumnInfo(name = "tipo_evento") val tipoEvento: String,
    @ColumnInfo(name = "estado_anterior") val estadoAnterior: String? = null,
    @ColumnInfo(name = "estado_nuevo") val estadoNuevo: String? = null,
    val motivo: String? = null,
    val nota: String? = null,
    @ColumnInfo(name = "registrado_en") val registradoEn: String,
    val actor: String
)
