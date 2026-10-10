package com.example.tallercelulares.DataBase.Entidades.TrabajoRealizado

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trabajo_realizado",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["orden_id"])]
)
data class TrabajoRealizado(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val descripcion: String,
    @ColumnInfo(name = "inicio_en") val inicioEn: String? = null,
    @ColumnInfo(name = "fin_en") val finEn: String? = null,
    @ColumnInfo(name = "costo_mano_obra_minor") val costoManoObraMinor: Long? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: String
)
