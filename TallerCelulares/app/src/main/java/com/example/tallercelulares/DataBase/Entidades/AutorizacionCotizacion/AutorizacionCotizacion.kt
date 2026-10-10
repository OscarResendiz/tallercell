package com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion

import com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden.AdjuntoOrden
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.Cotizacion
import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "autorizacion_cotizacion",
    foreignKeys = [
        ForeignKey(
            entity = Cotizacion::class,
            parentColumns = ["id"],
            childColumns = ["cotizacion_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AdjuntoOrden::class,
            parentColumns = ["id"],
            childColumns = ["evidencia_adjunto_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["cotizacion_id"]), Index(value = ["evidencia_adjunto_id"])]
)
data class AutorizacionCotizacion(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "cotizacion_id") val cotizacionId: String,
    val decision: String,
    val canal: String,
    @ColumnInfo(name = "alcance_confirmado") val alcanceConfirmado: String,
    @ColumnInfo(name = "evidencia_adjunto_id") val evidenciaAdjuntoId: String? = null,
    val nota: String? = null,
    @ColumnInfo(name = "registrada_en") val registradaEn: String
)
