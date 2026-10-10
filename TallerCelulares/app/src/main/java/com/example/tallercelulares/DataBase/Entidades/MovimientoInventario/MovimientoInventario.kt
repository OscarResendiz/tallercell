package com.example.tallercelulares.DataBase.Entidades.MovimientoInventario

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio
import com.example.tallercelulares.DataBase.Entidades.Pieza.Pieza

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimiento_inventario",
    foreignKeys = [
        ForeignKey(
            entity = Pieza::class,
            parentColumns = ["id"],
            childColumns = ["pieza_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["pieza_id", "creado_en"]), Index(value = ["orden_id"])]
)
data class MovimientoInventario(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "pieza_id") val piezaId: String,
    @ColumnInfo(name = "orden_id") val ordenId: String? = null,
    val tipo: String,
    val cantidad: Int,
    @ColumnInfo(name = "costo_unitario_minor") val costoUnitarioMinor: Long? = null,
    val proveedor: String? = null,
    @ColumnInfo(name = "numero_pedido") val numeroPedido: String? = null,
    @ColumnInfo(name = "estado_compra") val estadoCompra: String? = null,
    @ColumnInfo(name = "fecha_estimada") val fechaEstimada: String? = null,
    @ColumnInfo(name = "confirmada_en") val confirmadaEn: String? = null,
    val motivo: String? = null,
    @ColumnInfo(name = "creado_en") val creadoEn: String
)
