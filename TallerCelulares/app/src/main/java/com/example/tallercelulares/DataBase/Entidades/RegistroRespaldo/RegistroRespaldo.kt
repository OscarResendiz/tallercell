package com.example.tallercelulares.DataBase.Entidades.RegistroRespaldo

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "registro_respaldo")
data class RegistroRespaldo(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    @ColumnInfo(name = "creado_en") val creadoEn: String,
    @ColumnInfo(name = "version_formato") val versionFormato: String,
    val resultado: String,
    @ColumnInfo(name = "incluye_credenciales") val incluyeCredenciales: Boolean,
    val elementos: Int? = null,
    @ColumnInfo(name = "tamano_bytes") val tamanoBytes: Long? = null,
    @ColumnInfo(name = "ubicacion_etiqueta") val ubicacionEtiqueta: String? = null
)
