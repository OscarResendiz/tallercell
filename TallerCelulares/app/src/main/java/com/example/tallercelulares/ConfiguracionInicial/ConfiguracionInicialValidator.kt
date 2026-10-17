package com.example.tallercelulares.ConfiguracionInicial

import com.example.tallercelulares.DataBase.Entidades.ConfiguracionTaller.ConfiguracionTaller
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.Currency
import java.util.TimeZone

data class DatosTaller(
    val nombre: String,
    val telefono: String,
    val direccion: String,
    val monedaCodigo: String,
    val zonaHoraria: String,
    val prefijoFolio: String,
    val siguienteFolio: String,
    val impuestoPorcentaje: String,
    val cargoDiagnostico: String,
    val diasVigenciaCotizacion: String,
    val diasGarantia: String,
    val diasSeguimientoAutorizacion: String,
    val diasRefaccionAtrasada: String,
    val diasEquipoListo: String
)

sealed interface ResultadoValidacionTaller {
    data class Valido(val configuracion: ConfiguracionTaller) : ResultadoValidacionTaller
    data class Invalido(val campo: String, val mensaje: String) : ResultadoValidacionTaller
}

object ConfiguracionInicialValidator {
    fun validar(
        datos: DatosTaller,
        ahora: String = Instant.now().toString(),
        creadoEn: String? = null
    ): ResultadoValidacionTaller {
        val moneda = datos.monedaCodigo.trim().uppercase()
        val currency = try {
            Currency.getInstance(moneda)
        } catch (_: IllegalArgumentException) {
            return ResultadoValidacionTaller.Invalido("moneda", "Escribe un código de moneda ISO 4217 válido.")
        }

        val zonaHoraria = datos.zonaHoraria.trim()
        if (zonaHoraria !in TimeZone.getAvailableIDs()) {
            return ResultadoValidacionTaller.Invalido("zona_horaria", "Selecciona una zona horaria válida.")
        }

        val prefijo = datos.prefijoFolio.trim()
        if (!prefijo.matches(Regex("[A-Za-z0-9-]{1,10}"))) {
            return ResultadoValidacionTaller.Invalido(
                "prefijo_folio",
                "Usa de 1 a 10 letras, números o guiones."
            )
        }

        val siguienteFolio = datos.siguienteFolio.trim().toLongOrNull()
            ?: return ResultadoValidacionTaller.Invalido("siguiente_folio", "Escribe un número de folio válido.")
        if (siguienteFolio < 1) {
            return ResultadoValidacionTaller.Invalido("siguiente_folio", "El siguiente folio debe ser mayor que cero.")
        }

        val impuesto = decimal(datos.impuestoPorcentaje)
            ?: return ResultadoValidacionTaller.Invalido("impuesto", "Escribe una tasa numérica válida.")
        if (impuesto < BigDecimal.ZERO || impuesto > BigDecimal("100")) {
            return ResultadoValidacionTaller.Invalido("impuesto", "La tasa debe estar entre 0 y 100 por ciento.")
        }

        val fraccionMonetaria = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
        val cargo = decimal(datos.cargoDiagnostico)
            ?: return ResultadoValidacionTaller.Invalido("cargo_diagnostico", "Escribe un importe válido.")
        if (cargo < BigDecimal.ZERO) {
            return ResultadoValidacionTaller.Invalido("cargo_diagnostico", "El cargo no puede ser negativo.")
        }
        val cargoMinor = try {
            cargo.setScale(fraccionMonetaria, RoundingMode.UNNECESSARY)
                .movePointRight(fraccionMonetaria)
                .longValueExact()
        } catch (_: ArithmeticException) {
            return ResultadoValidacionTaller.Invalido(
                "cargo_diagnostico",
                "El importe excede la precisión permitida para $moneda."
            )
        }

        val vigencia = dias(datos.diasVigenciaCotizacion, minimo = 1)
            ?: return ResultadoValidacionTaller.Invalido("vigencia", "La vigencia debe ser de 1 a 3650 días.")
        val garantia = dias(datos.diasGarantia, minimo = 0)
            ?: return ResultadoValidacionTaller.Invalido("garantia", "La garantía debe ser de 0 a 3650 días.")
        val seguimiento = dias(datos.diasSeguimientoAutorizacion, minimo = 0)
            ?: return ResultadoValidacionTaller.Invalido(
                "seguimiento_autorizacion",
                "El plazo debe ser de 0 a 3650 días."
            )
        val refaccion = dias(datos.diasRefaccionAtrasada, minimo = 0)
            ?: return ResultadoValidacionTaller.Invalido(
                "refaccion_atrasada",
                "El plazo debe ser de 0 a 3650 días."
            )
        val equipoListo = dias(datos.diasEquipoListo, minimo = 0)
            ?: return ResultadoValidacionTaller.Invalido("equipo_listo", "El plazo debe ser de 0 a 3650 días.")

        return ResultadoValidacionTaller.Valido(
            ConfiguracionTaller(
                nombre = datos.nombre.trim().ifEmpty { null },
                telefono = datos.telefono.trim().ifEmpty { null },
                direccion = datos.direccion.trim().ifEmpty { null },
                monedaCodigo = moneda,
                zonaHoraria = zonaHoraria,
                prefijoFolio = prefijo,
                siguienteFolio = siguienteFolio,
                impuestoTasa = impuesto.movePointLeft(2).stripTrailingZeros().toPlainString(),
                cargoDiagnosticoMinor = cargoMinor,
                diasVigenciaCotizacion = vigencia,
                diasGarantiaPredeterminados = garantia,
                diasRecordatorioSeguimientoAutorizacion = seguimiento,
                diasRecordatorioRefaccionAtrasada = refaccion,
                diasRecordatorioEquipoListo = equipoListo,
                creadoEn = creadoEn ?: ahora,
                actualizadoEn = ahora
            )
        )
    }

    private fun decimal(raw: String): BigDecimal? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val normalized = when {
            ',' in value && '.' in value -> return null
            ',' in value -> value.replace(',', '.')
            else -> value
        }
        return normalized.toBigDecimalOrNull()
    }

    private fun dias(raw: String, minimo: Int): Int? {
        val valor = raw.trim().toIntOrNull() ?: return null
        if (valor !in minimo..3650) return null
        return valor
    }
}
