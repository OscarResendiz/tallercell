package com.example.tallercelulares.DataBase.Entidades.OrdenServicio

import androidx.room.ColumnInfo

data class OrdenTableroRegistro(
    val id: String,
    val folio: String,
    @ColumnInfo(name = "cliente_nombre_snapshot") val cliente: String,
    @ColumnInfo(name = "cliente_contacto_snapshot") val contacto: String?,
    @ColumnInfo(name = "tipo_equipo_snapshot") val tipoEquipo: String,
    @ColumnInfo(name = "marca_snapshot") val marca: String,
    @ColumnInfo(name = "modelo_snapshot") val modelo: String,
    @ColumnInfo(name = "serie_snapshot") val serie: String?,
    @ColumnInfo(name = "imei_snapshot") val imei: String?,
    val estado: String,
    @ColumnInfo(name = "recibida_en") val recibidaEn: String,
    @ColumnInfo(name = "fecha_prometida") val fechaPrometida: String?,
    @ColumnInfo(name = "estado_desde") val estadoDesde: String
)

data class ConteoOrdenTablero(
    val estado: String,
    val cantidad: Int
)
