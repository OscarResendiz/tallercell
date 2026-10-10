package com.example.tallercelulares.DataBase.Entidades.CredencialEquipo

import com.example.tallercelulares.DataBase.Entidades.EntidadId
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credencial_equipo",
    foreignKeys = [
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["orden_id"], unique = true)]
)
data class CredencialEquipo(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "orden_id") val ordenId: String,
    val tipo: String,
    @ColumnInfo(name = "valor_cifrado") val valorCifrado: ByteArray? = null,
    val nonce: ByteArray? = null,
    @ColumnInfo(name = "version_clave") val versionClave: String? = null,
    @ColumnInfo(name = "consentimiento_en") val consentimientoEn: String,
    val proposito: String,
    @ColumnInfo(name = "eliminar_en") val eliminarEn: String,
    @ColumnInfo(name = "eliminada_en") val eliminadaEn: String? = null
)
