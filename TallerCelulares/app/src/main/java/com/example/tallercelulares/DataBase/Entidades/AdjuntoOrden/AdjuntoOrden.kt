package com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "adjunto_orden",
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
data class AdjuntoOrden(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val categoria: String,
    @ColumnInfo(name = "ruta_relativa") val rutaRelativa: String,
    @ColumnInfo(name = "nombre_original") val nombreOriginal: String? = null,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "tamano_bytes") val tamanoBytes: Long,
    @ColumnInfo(name = "hash_contenido") val hashContenido: String,
    @ColumnInfo(name = "consentimiento_registrado") val consentimientoRegistrado: Boolean,
    @ColumnInfo(name = "creado_en") val creadoEn: String
)
