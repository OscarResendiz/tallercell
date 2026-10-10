package com.example.tallercelulares.DataBase.Entidades.AccesorioOrden

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accesorio_orden",
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
data class AccesorioOrden(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val descripcion: String,
    val cantidad: Int,
    @ColumnInfo(name = "condicion_ingreso") val condicionIngreso: String? = null,
    val devuelto: Boolean = false,
    @ColumnInfo(name = "condicion_salida") val condicionSalida: String? = null,
    val nota: String? = null
)
