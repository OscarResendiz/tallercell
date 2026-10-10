package com.example.tallercelulares.DataBase.Entidades.OrdenServicio

import com.example.tallercelulares.DataBase.Entidades.Cliente.Cliente
import com.example.tallercelulares.DataBase.Entidades.Dispositivo.Dispositivo
import com.example.tallercelulares.DataBase.Entidades.EntidadId

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orden_servicio",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["cliente_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Dispositivo::class,
            parentColumns = ["id"],
            childColumns = ["dispositivo_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = OrdenServicio::class,
            parentColumns = ["id"],
            childColumns = ["orden_origen_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["folio"], unique = true),
        Index(value = ["estado", "recibida_en"]),
        Index(value = ["cliente_id"]),
        Index(value = ["dispositivo_id"]),
        Index(value = ["orden_origen_id"])
    ]
)
data class OrdenServicio(
    @PrimaryKey val id: String = EntidadId.nuevo(),
    val folio: String,
    @ColumnInfo(name = "cliente_id") val clienteId: String? = null,
    @ColumnInfo(name = "dispositivo_id") val dispositivoId: String? = null,
    @ColumnInfo(name = "orden_origen_id") val ordenOrigenId: String? = null,
    @ColumnInfo(name = "tipo_relacion_origen") val tipoRelacionOrigen: String? = null,
    @ColumnInfo(name = "cliente_nombre_snapshot") val clienteNombreSnapshot: String,
    @ColumnInfo(name = "cliente_contacto_snapshot") val clienteContactoSnapshot: String? = null,
    @ColumnInfo(name = "tipo_equipo_snapshot") val tipoEquipoSnapshot: String,
    @ColumnInfo(name = "marca_snapshot") val marcaSnapshot: String,
    @ColumnInfo(name = "modelo_snapshot") val modeloSnapshot: String,
    @ColumnInfo(name = "color_snapshot") val colorSnapshot: String,
    @ColumnInfo(name = "serie_snapshot") val serieSnapshot: String? = null,
    @ColumnInfo(name = "imei_snapshot") val imeiSnapshot: String? = null,
    @ColumnInfo(name = "falla_reportada") val fallaReportada: String,
    @ColumnInfo(name = "condicion_ingreso") val condicionIngreso: String,
    val estado: String,
    @ColumnInfo(name = "resultado_tecnico") val resultadoTecnico: String? = null,
    @ColumnInfo(name = "resultado_comercial") val resultadoComercial: String? = null,
    @ColumnInfo(name = "ubicacion_resguardo") val ubicacionResguardo: String? = null,
    @ColumnInfo(name = "recibida_en") val recibidaEn: String,
    @ColumnInfo(name = "fecha_prometida") val fechaPrometida: String? = null,
    @ColumnInfo(name = "entregada_en") val entregadaEn: String? = null,
    @ColumnInfo(name = "cerrada_en") val cerradaEn: String? = null,
    @ColumnInfo(name = "creada_en") val creadaEn: String,
    @ColumnInfo(name = "actualizada_en") val actualizadaEn: String,
    @ColumnInfo(name = "eliminada_en") val eliminadaEn: String? = null
)
