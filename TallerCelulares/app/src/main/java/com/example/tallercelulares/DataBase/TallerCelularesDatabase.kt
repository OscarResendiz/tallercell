package com.example.tallercelulares.DataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tallercelulares.DataBase.Entidades.AccesorioOrden.AccesorioOrden
import com.example.tallercelulares.DataBase.Entidades.AccesorioOrden.AccesorioOrdenDao
import com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden.AdjuntoOrden
import com.example.tallercelulares.DataBase.Entidades.AdjuntoOrden.AdjuntoOrdenDao
import com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion.AutorizacionCotizacion
import com.example.tallercelulares.DataBase.Entidades.AutorizacionCotizacion.AutorizacionCotizacionDao
import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicio
import com.example.tallercelulares.DataBase.Entidades.CatalogoServicio.CatalogoServicioDao
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
        CatalogoServicio::class,
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
    version = 1,
    exportSchema = false
)
abstract class TallerCelularesDatabase : RoomDatabase() {
    abstract fun accesorioOrdenDao(): AccesorioOrdenDao
    abstract fun adjuntoOrdenDao(): AdjuntoOrdenDao
    abstract fun autorizacionCotizacionDao(): AutorizacionCotizacionDao
    abstract fun catalogoServicioDao(): CatalogoServicioDao
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
                ).build().also { instancia = it }
            }
    }
}
