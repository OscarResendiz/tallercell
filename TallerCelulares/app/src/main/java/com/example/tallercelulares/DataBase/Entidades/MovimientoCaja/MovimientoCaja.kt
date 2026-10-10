package com.example.tallercelulares.DataBase.Entidades.MovimientoCaja

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimiento_caja",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = MovimientoCaja::class,
            parentColumns = ["id"],
            childColumns = ["movimiento_origen_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["orden_id", "registrado_en"]), Index(value = ["movimiento_origen_id"])]
)
data class MovimientoCaja(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String? = null,
    @ColumnInfo(name = "movimiento_origen_id") val movimientoOrigenId: String? = null,
    val tipo: String,
    val direccion: String,
    @ColumnInfo(name = "importe_minor") val importeMinor: Long,
    @ColumnInfo(name = "moneda_codigo") val monedaCodigo: String,
    @ColumnInfo(name = "metodo_pago") val metodoPago: String,
    @ColumnInfo(name = "referencia_no_sensible") val referenciaNoSensible: String? = null,
    val descripcion: String? = null,
    @ColumnInfo(name = "registrado_en") val registradoEn: String
)
