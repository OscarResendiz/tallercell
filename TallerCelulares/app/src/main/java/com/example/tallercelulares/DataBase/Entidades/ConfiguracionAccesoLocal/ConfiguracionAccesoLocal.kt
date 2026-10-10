package com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal

import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "configuracion_acceso_local")
data class ConfiguracionAccesoLocal(
    @PrimaryKey val id: String = "ACCESO_LOCAL",
    val metodo: String,
    @ColumnInfo(name = "verificador_contrasena") val verificadorContrasena: ByteArray? = null,
    @ColumnInfo(name = "sal_contrasena") val salContrasena: ByteArray? = null,
    @ColumnInfo(name = "algoritmo_contrasena") val algoritmoContrasena: String? = null,
    @ColumnInfo(name = "alias_clave_keystore") val aliasClaveKeystore: String? = null,
    @ColumnInfo(name = "tiempo_bloqueo_segundos") val tiempoBloqueoSegundos: Int,
    @ColumnInfo(name = "actualizado_en") val actualizadoEn: String
)
