package com.example.tallercelulares.Modulos.Catalogos.Servicios

import com.example.tallercelulares.DataBase.Entidades.DetalleCotizacion.DetalleCotizacion
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal data class TotalesCotizacionServicio(
    val subtotalMinor: Long,
    val impuestoMinor: Long,
    val totalMinor: Long
)

internal object ReglasServicios {
    private const val MAX_CODIGO = 40
    private const val MAX_NOMBRE = 120
    private const val MAX_DESCRIPCION = 1_000
    private val patronDecimal = Regex("""(?:\d+(?:[.,]\d*)?|[.,]\d+)""")

    fun validar(
        codigo: String,
        nombre: String,
        descripcion: String,
        precio: String,
        monedaCodigo: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        validarOpcional("codigo", codigo, MAX_CODIGO, errores)
        validarRequerido("nombre", nombre, MAX_NOMBRE, "El nombre es obligatorio.", errores)
        validarRequerido(
            "descripcion",
            descripcion,
            MAX_DESCRIPCION,
            "La descripción es obligatoria.",
            errores
        )
        if (precio.trim().isEmpty()) {
            errores["precio"] = "El precio base es obligatorio."
        } else if (precioBaseMinor(precio, monedaCodigo) == null) {
            errores["precio"] = "Escribe un precio válido para la moneda del taller."
        }
        return errores
    }

    fun normalizarCodigo(codigo: String): String? =
        codigo.trim().takeIf(String::isNotEmpty)

    fun patronBusqueda(consulta: String): String = consulta.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    fun precioBaseMinor(precio: String, monedaCodigo: String): Long? {
        val texto = precio.trim()
        if (!patronDecimal.matches(texto)) return null
        val normalizado = texto.replace(',', '.')
        val moneda = try {
            Currency.getInstance(monedaCodigo)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val digitosFraccionarios = moneda.defaultFractionDigits
        if (digitosFraccionarios < 0) return null
        val importe = normalizado.toBigDecimalOrNull() ?: return null
        if (importe.signum() < 0) return null
        return try {
            importe.setScale(digitosFraccionarios, RoundingMode.UNNECESSARY)
                .movePointRight(digitosFraccionarios)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun textoPrecioBase(precioBaseMinor: Long, monedaCodigo: String): String {
        val digitosFraccionarios = Currency.getInstance(monedaCodigo).defaultFractionDigits
        require(digitosFraccionarios >= 0) { "La moneda no admite conversión a unidades menores." }
        return BigDecimal.valueOf(precioBaseMinor, digitosFraccionarios).toPlainString()
    }

    fun formatearPrecio(precioBaseMinor: Long, monedaCodigo: String): String {
        val moneda = Currency.getInstance(monedaCodigo)
        val digitosFraccionarios = moneda.defaultFractionDigits
        require(digitosFraccionarios >= 0) { "La moneda no admite formato monetario." }
        val formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).apply {
            currency = moneda
            minimumFractionDigits = digitosFraccionarios
            maximumFractionDigits = digitosFraccionarios
        }
        return formato.format(BigDecimal.valueOf(precioBaseMinor, digitosFraccionarios))
    }

    fun calcularTotales(
        detalles: List<DetalleCotizacion>,
        descuentoMinor: Long,
        impuestoTasa: String
    ): TotalesCotizacionServicio {
        val subtotalMinor = detalles.fold(0L) { acumulado, detalle ->
            require(detalle.cantidad > 0) { "La cantidad del detalle debe ser positiva." }
            require(detalle.importeMinor >= 0) { "El importe del detalle no puede ser negativo." }
            Math.addExact(acumulado, detalle.importeMinor)
        }
        require(descuentoMinor in 0..subtotalMinor) {
            "El descuento no puede exceder el subtotal."
        }
        val tasa = impuestoTasa.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("La tasa de impuesto configurada no es válida.")
        require(tasa >= BigDecimal.ZERO && tasa <= BigDecimal.ONE) {
            "La tasa de impuesto configurada está fuera de rango."
        }
        val baseGravable = Math.subtractExact(subtotalMinor, descuentoMinor)
        val impuestoMinor = BigDecimal.valueOf(baseGravable)
            .multiply(tasa)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
        return TotalesCotizacionServicio(
            subtotalMinor = subtotalMinor,
            impuestoMinor = impuestoMinor,
            totalMinor = Math.addExact(baseGravable, impuestoMinor)
        )
    }

    private fun validarRequerido(
        campo: String,
        valor: String,
        maximo: Int,
        mensajeVacio: String,
        errores: MutableMap<String, String>
    ) {
        when {
            valor.trim().isEmpty() -> errores[campo] = mensajeVacio
            valor.trim().length > maximo -> errores[campo] = "Máximo $maximo caracteres."
        }
    }

    private fun validarOpcional(
        campo: String,
        valor: String,
        maximo: Int,
        errores: MutableMap<String, String>
    ) {
        if (valor.trim().length > maximo) errores[campo] = "Máximo $maximo caracteres."
    }
}
