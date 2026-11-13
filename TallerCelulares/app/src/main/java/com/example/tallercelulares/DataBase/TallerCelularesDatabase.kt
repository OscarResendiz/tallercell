package com.example.tallercelulares.DataBase

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tallercelulares.DataBase.Entidades.AccesorioOrden.AccesorioOrden
import com.example.tallercelulares.DataBase.Entidades.AccesorioOrden.AccesorioOrdenDao
import com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden.AdjuntoOrden
import com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden.AdjuntoOrdenDao
import com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion.AutorizacionCotizacion
import com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion.AutorizacionCotizacionDao
import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicio
import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicioDao
import com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema.CatalogoMonedaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema.CatalogoMonedaSistemaDao
import com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema.CatalogoZonaHorariaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema.CatalogoZonaHorariaSistemaDao
import com.example.tallercelulares.DataBase.Entidades.Cliente.Cliente
import com.example.tallercelulares.DataBase.Entidades.Cliente.ClienteDao
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal.ConfiguracionAccesoLocal
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionAccesoLocal.ConfiguracionAccesoLocalDao
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTaller
import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTallerDao
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.Cotizacion
import com.example.tallercelulares.DataBase.Entidades.Cotizacion.CotizacionDao
import com.example.tallercelulares.DataBase.Entidades.CredencialEquipo.CredencialEquipo
import com.example.tallercelulares.DataBase.Entidades.CredencialEquipo.CredencialEquipoDao
import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacion
import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacionDao
import com.example.tallercelulares.DataBase.Entidades.DiagnosticoOrden.DiagnosticoOrden
import com.example.tallercelulares.DataBase.Entidades.DiagnosticoOrden.DiagnosticoOrdenDao
import com.example.tallercelulares.DataBase.Entidades.Dispositivo.Dispositivo
import com.example.tallercelulares.DataBase.Entidades.Dispositivo.DispositivoDao
import com.example.tallercelulares.DataBase.Entidades.EventoOrden.EventoOrden
import com.example.tallercelulares.DataBase.Entidades.EventoOrden.EventoOrdenDao
import com.example.tallercelulares.DataBase.Entidades.Garantia.Garantia
import com.example.tallercelulares.DataBase.Entidades.Garantia.GarantiaDao
import com.example.tallercelulares.DataBase.Entidades.MovimientoCaja.MovimientoCaja
import com.example.tallercelulares.DataBase.Entidades.MovimientoCaja.MovimientoCajaDao
import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventario
import com.example.tallercelulares.DataBase.Entidades.MovimientoInventario.MovimientoInventarioDao
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicio
import com.example.tallercelulares.DataBase.Entidades.OrdenServicio.OrdenServicioDao
import com.example.tallercelulares.DataBase.Entidades.Pieza.Pieza
import com.example.tallercelulares.DataBase.Entidades.Pieza.PiezaDao
import com.example.tallercelulares.DataBase.Entidades.PruebaOrden.PruebaOrden
import com.example.tallercelulares.DataBase.Entidades.PruebaOrden.PruebaOrdenDao
import com.example.tallercelulares.DataBase.Entidades.RecordatorioLocal.RecordatorioLocal
import com.example.tallercelulares.DataBase.Entidades.RecordatorioLocal.RecordatorioLocalDao
import com.example.tallercelulares.DataBase.Entidades.RegistroRespaldo.RegistroRespaldo
import com.example.tallercelulares.DataBase.Entidades.RegistroRespaldo.RegistroRespaldoDao
import com.example.tallercelulares.DataBase.Entidades.TrabajoRealizado.TrabajoRealizado
import com.example.tallercelulares.DataBase.Entidades.TrabajoRealizado.TrabajoRealizadoDao

@Database(
    entities = [
        AccesorioOrden::class,
        AdjuntoOrden::class,
        AutorizacionCotizacion::class,
        CatalogoMonedaSistema::class,
        CatalogoServicio::class,
        CatalogoZonaHorariaSistema::class,
        Cliente::class,
        ConfiguracionAccesoLocal::class,
        ConfiguracionTaller::class,
        Cotizacion::class,
        CredencialEquipo::class,
        DetalleCotizacion::class,
        DiagnosticoOrden::class,
        Dispositivo::class,
        EventoOrden::class,
        Garantia::class,
        MovimientoCaja::class,
        MovimientoInventario::class,
        OrdenServicio::class,
        Pieza::class,
        PruebaOrden::class,
        RecordatorioLocal::class,
        RegistroRespaldo::class,
        TrabajoRealizado::class
    ],
    version = 4,
    exportSchema = false
)
abstract class TallerCelularesDatabase : RoomDatabase() {
    abstract fun accesorioOrdenDao(): AccesorioOrdenDao
    abstract fun adjuntoOrdenDao(): AdjuntoOrdenDao
    abstract fun autorizacionCotizacionDao(): AutorizacionCotizacionDao
    abstract fun catalogoMonedaSistemaDao(): CatalogoMonedaSistemaDao
    abstract fun catalogoServicioDao(): CatalogoServicioDao
    abstract fun catalogoZonaHorariaSistemaDao(): CatalogoZonaHorariaSistemaDao
    abstract fun clienteDao(): ClienteDao
    abstract fun configuracionAccesoLocalDao(): ConfiguracionAccesoLocalDao
    abstract fun configuracionTallerDao(): ConfiguracionTallerDao
    abstract fun cotizacionDao(): CotizacionDao
    abstract fun credencialEquipoDao(): CredencialEquipoDao
    abstract fun detalleCotizacionDao(): DetalleCotizacionDao
    abstract fun diagnosticoOrdenDao(): DiagnosticoOrdenDao
    abstract fun dispositivoDao(): DispositivoDao
    abstract fun eventoOrdenDao(): EventoOrdenDao
    abstract fun garantiaDao(): GarantiaDao
    abstract fun movimientoCajaDao(): MovimientoCajaDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao
    abstract fun ordenServicioDao(): OrdenServicioDao
    abstract fun piezaDao(): PiezaDao
    abstract fun pruebaOrdenDao(): PruebaOrdenDao
    abstract fun recordatorioLocalDao(): RecordatorioLocalDao
    abstract fun registroRespaldoDao(): RegistroRespaldoDao
    abstract fun trabajoRealizadoDao(): TrabajoRealizadoDao

    companion object {
        @Volatile
        private var instancia: TallerCelularesDatabase? = null

        fun obtenerInstancia(context: Context): TallerCelularesDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    TallerCelularesDatabase::class.java,
                    "taller_celulares.db"
                )
                    .addMigrations(MIGRACION_1_2)
                    .addMigrations(MIGRACION_2_3)
                    .addMigrations(MIGRACION_3_4)
                    .build()
                    .also { instancia = it }
            }

        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalogo_moneda_sistema` (
                        `codigo` TEXT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `simbolo` TEXT NOT NULL,
                        `activo` INTEGER NOT NULL,
                        PRIMARY KEY(`codigo`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `catalogo_zona_horaria_sistema` (
                        `id` TEXT NOT NULL,
                        `nombre` TEXT NOT NULL,
                        `activo` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }

        }

        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `dispositivo` ADD COLUMN `eliminado_en` TEXT"
                )
            }
        }

        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE `movimiento_inventario_nuevo` (
                        `id` TEXT NOT NULL,
                        `pieza_id` TEXT NOT NULL,
                        `orden_id` TEXT,
                        `tipo` TEXT NOT NULL,
                        `cantidad` INTEGER NOT NULL,
                        `costo_unitario_minor` INTEGER,
                        `proveedor` TEXT,
                        `numero_pedido` TEXT,
                        `estado_compra` TEXT,
                        `fecha_estimada` TEXT,
                        `confirmada_en` TEXT,
                        `motivo` TEXT,
                        `creado_en` TEXT NOT NULL,
                        `sentido_ajuste` TEXT,
                        `movimiento_origen_id` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`pieza_id`) REFERENCES `pieza`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`orden_id`) REFERENCES `orden_servicio`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`movimiento_origen_id`) REFERENCES `movimiento_inventario_nuevo`(`id`)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `movimiento_inventario_nuevo` (
                        `id`, `pieza_id`, `orden_id`, `tipo`, `cantidad`,
                        `costo_unitario_minor`, `proveedor`, `numero_pedido`,
                        `estado_compra`, `fecha_estimada`, `confirmada_en`,
                        `motivo`, `creado_en`, `sentido_ajuste`, `movimiento_origen_id`
                    )
                    SELECT `id`, `pieza_id`, `orden_id`, `tipo`, `cantidad`,
                        `costo_unitario_minor`, `proveedor`, `numero_pedido`,
                        `estado_compra`, `fecha_estimada`, `confirmada_en`,
                        `motivo`, `creado_en`, NULL, NULL
                    FROM `movimiento_inventario`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE `movimiento_inventario`")
                database.execSQL(
                    "ALTER TABLE `movimiento_inventario_nuevo` RENAME TO `movimiento_inventario`"
                )
                database.execSQL(
                    "CREATE INDEX `index_movimiento_inventario_pieza_id_creado_en` " +
                        "ON `movimiento_inventario` (`pieza_id`, `creado_en`)"
                )
                database.execSQL(
                    "CREATE INDEX `index_movimiento_inventario_orden_id` " +
                        "ON `movimiento_inventario` (`orden_id`)"
                )
                database.execSQL(
                    "CREATE INDEX `index_movimiento_inventario_movimiento_origen_id` " +
                        "ON `movimiento_inventario` (`movimiento_origen_id`)"
                )
            }
        }
    }
}
