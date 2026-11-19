package com.example.tallercelulares.Modulos.Configuracion

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.TimeZone

data class CamposConfiguracionTaller(
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
    val diasEquipoListo: String,
    val textoRecepcion: String,
    val textoAutorizacion: String,
    val textoGarantia: String
)

data class ValoresConfiguracionValidados(
    val nombre: String?,
    val telefono: String?,
    val direccion: String?,
    val monedaCodigo: String,
    val zonaHoraria: String,
    val prefijoFolio: String,
    val siguienteFolio: Long,
    val impuestoTasa: String,
    val cargoDiagnosticoMinor: Long,
    val diasVigenciaCotizacion: Int,
    val diasGarantia: Int,
    val diasSeguimientoAutorizacion: Int,
    val diasRefaccionAtrasada: Int,
    val diasEquipoListo: Int,
    val textoRecepcion: String?,
    val textoAutorizacion: String?,
    val textoGarantia: String?
)

object ReglasConfiguracion {
    private const val MAX_NOMBRE = 120
    private const val MAX_TELEFONO = 40
    private const val MAX_DIRECCION = 300
    private const val MAX_TEXTO_DOCUMENTO = 2_000

    fun validar(c: CamposConfiguracionTaller): Map<String, String> {
        val errores = mutableMapOf<String, String>()

        validarLongitud("nombre", c.nombre, MAX_NOMBRE, errores)
        validarLongitud("telefono", c.telefono, MAX_TELEFONO, errores)
        validarLongitud("direccion", c.direccion, MAX_DIRECCION, errores)

        val moneda = c.monedaCodigo.trim().uppercase()
        val currency = try {
            Currency.getInstance(moneda)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (currency == null) {
            errores["moneda"] = "Selecciona una moneda válida."
        }

        if (c.zonaHoraria.trim() !in TimeZone.getAvailableIDs()) {
            errores["zona_horaria"] = "Selecciona una zona horaria válida."
        }

        val prefijo = c.prefijoFolio.trim()
        if (!prefijo.matches(Regex("[A-Za-z0-9-]{1,10}"))) {
            errores["prefijo_folio"] = "Usa de 1 a 10 letras, números o guiones."
        }

        val siguiente = c.siguienteFolio.trim().toLongOrNull()
        if (siguiente == null || siguiente < 1L) {
            errores["siguiente_folio"] = "El siguiente folio debe ser mayor que cero."
        }

        val impuesto = decimal(c.impuestoPorcentaje)
        if (impuesto == null || impuesto < BigDecimal.ZERO || impuesto > BigDecimal("100")) {
            errores["impuesto"] = "La tasa debe estar entre 0 y 100 por ciento."
        }

        val cargo = decimal(c.cargoDiagnostico)
        val digitos = currency?.defaultFractionDigits?.takeIf { it >= 0 }
        if (cargo == null || cargo < BigDecimal.ZERO) {
            errores["cargo_diagnostico"] = "Escribe un importe válido y no negativo."
        } else if (digitos != null) {
            val minor = runCatching {
                cargo.setScale(digitos, RoundingMode.UNNECESSARY)
                    .movePointRight(digitos)
                    .longValueExact()
            }.getOrNull()
            if (minor == null) {
                errores["cargo_diagnostico"] =
                    "El importe excede la precisión o el rango permitido para $moneda."
            }
        }

        validarDias("vigencia", c.diasVigenciaCotizacion, minimo = 1, errores)
        validarDias("garantia", c.diasGarantia, minimo = 0, errores)
        validarDias("seguimiento", c.diasSeguimientoAutorizacion, minimo = 0, errores)
        validarDias("refaccion_atrasada", c.diasRefaccionAtrasada, minimo = 0, errores)
        validarDias("equipo_listo", c.diasEquipoListo, minimo = 0, errores)

        validarLongitud("texto_recepcion", c.textoRecepcion, MAX_TEXTO_DOCUMENTO, errores)
        validarLongitud("texto_autorizacion", c.textoAutorizacion, MAX_TEXTO_DOCUMENTO, errores)
        validarLongitud("texto_garantia", c.textoGarantia, MAX_TEXTO_DOCUMENTO, errores)

        return errores
    }

    fun validarMetodoPago(nombre: String): String? {
        val limpio = nombre.trim()
        return when {
            limpio.isEmpty() -> "El nombre del método es obligatorio."
            limpio.length > 40 -> "Usa máximo 40 caracteres."
            else -> null
        }
    }

    fun textoOpcional(valor: String): String? =
        valor.trim().takeIf(String::isNotEmpty)

    fun convertir(c: CamposConfiguracionTaller): ValoresConfiguracionValidados? {
        if (validar(c).isNotEmpty()) return null
        val moneda = c.monedaCodigo.trim().uppercase()
        val currency = Currency.getInstance(moneda)
        val digitos = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
        val cargo = decimal(c.cargoDiagnostico)!!.setScale(digitos, RoundingMode.UNNECESSARY)
            .movePointRight(digitos).longValueExact()
        val impuesto = decimal(c.impuestoPorcentaje)!!
            .movePointLeft(2).stripTrailingZeros().toPlainString()

        return ValoresConfiguracionValidados(
            nombre = textoOpcional(c.nombre),
            telefono = textoOpcional(c.telefono),
            direccion = textoOpcional(c.direccion),
            monedaCodigo = moneda,
            zonaHoraria = c.zonaHoraria.trim(),
            prefijoFolio = c.prefijoFolio.trim(),
            siguienteFolio = c.siguienteFolio.trim().toLong(),
            impuestoTasa = impuesto,
            cargoDiagnosticoMinor = cargo,
            diasVigenciaCotizacion = c.diasVigenciaCotizacion.trim().toInt(),
            diasGarantia = c.diasGarantia.trim().toInt(),
            diasSeguimientoAutorizacion = c.diasSeguimientoAutorizacion.trim().toInt(),
            diasRefaccionAtrasada = c.diasRefaccionAtrasada.trim().toInt(),
            diasEquipoListo = c.diasEquipoListo.trim().toInt(),
            textoRecepcion = textoOpcional(c.textoRecepcion),
            textoAutorizacion = textoOpcional(c.textoAutorizacion),
            textoGarantia = textoOpcional(c.textoGarantia)
        )
    }

    private fun decimal(valor: String): BigDecimal? {
        val texto = valor.trim()
        if (texto.isEmpty()) return null
        val normalizado = when {
            ',' in texto && '.' in texto -> return null
            ',' in texto -> texto.replace(',', '.')
            else -> texto
        }
        return normalizado.toBigDecimalOrNull()
    }

    private fun validarDias(
        clave: String,
        texto: String,
        minimo: Int,
        errores: MutableMap<String, String>
    ) {
        val valor = texto.trim().toIntOrNull()
        if (valor == null || valor !in minimo..3650) {
            errores[clave] = "El plazo debe ser de $minimo a 3650 días."
        }
    }

    private fun validarLongitud(
        clave: String,
        texto: String,
        maximo: Int,
        errores: MutableMap<String, String>
    ) {
        if (texto.trim().length > maximo) {
            errores[clave] = "Usa máximo $maximo caracteres."
        }
    }
}
