package com.example.tallercelulares.DataBase.Entidades.RecordatorioLocal

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recordatorio_local",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["programado_en", "estado"]), Index(value = ["orden_id"])]
)
data class RecordatorioLocal(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val tipo: String,
    @ColumnInfo(name = "programado_en") val programadoEn: String,
    @ColumnInfo(name = "completado_en") val completadoEn: String? = null,
    val estado: String,
    val nota: String? = null
)
