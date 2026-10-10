package com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion

import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicio
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.Cotizacion
import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.Pieza.Pieza

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalle_cotizacion",
    foreignKeys = [
        ForeignKey(
            entity = Cotizacion::class,
            parentColumns = ["id"],
            childColumns = ["cotizacion_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = CatalogoServicio::class,
            parentColumns = ["id"],
            childColumns = ["servicio_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Pieza::class,
            parentColumns = ["id"],
            childColumns = ["pieza_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["cotizacion_id"]),
        Index(value = ["servicio_id"]),
        Index(value = ["pieza_id"])
    ]
)
data class DetalleCotizacion(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "cotizacion_id") val cotizacionId: String,
    @ColumnInfo(name = "servicio_id") val servicioId: String? = null,
    val tipo: String,
    @ColumnInfo(name = "descripcion_snapshot") val descripcionSnapshot: String,
    val cantidad: Int,
    @ColumnInfo(name = "precio_unitario_minor") val precioUnitarioMinor: Long,
    @ColumnInfo(name = "importe_minor") val importeMinor: Long,
    @ColumnInfo(name = "pieza_id") val piezaId: String? = null
)
