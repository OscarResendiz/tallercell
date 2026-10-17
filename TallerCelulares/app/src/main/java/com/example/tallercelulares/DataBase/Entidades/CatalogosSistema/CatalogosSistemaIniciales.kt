package com.example.tallercelulares.DataBase.Entidades.CatalogosSistema

import com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema.CatalogoMonedaSistema
import com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema.CatalogoZonaHorariaSistema
import java.time.ZoneId
import java.util.Currency
import java.util.Locale

object CatalogosSistemaIniciales {
    private val codigosMonedaComunes = listOf(
        "AED", "ARS", "AUD", "BOB", "BRL", "CAD", "CHF", "CLP", "CNY", "COP",
        "CRC", "CZK", "DKK", "DOP", "EGP", "EUR", "GBP", "GTQ", "HKD", "HNL",
        "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NIO", "NOK",
        "NZD", "PAB", "PEN", "PHP", "PLN", "PYG", "QAR", "RON", "RUB", "SAR",
        "SEK", "SGD", "THB", "TRY", "TWD", "UAH", "USD", "UYU", "VND", "ZAR"
    )

    private val zonasHorariasComunes = listOf(
        "UTC",
        "America/Anchorage",
        "America/Argentina/Buenos_Aires",
        "America/Bogota",
        "America/Caracas",
        "America/Chicago",
        "America/Costa_Rica",
        "America/Denver",
        "America/Guatemala",
        "America/Halifax",
        "America/Hermosillo",
        "America/Lima",
        "America/Los_Angeles",
        "America/Mexico_City",
        "America/Monterrey",
        "America/New_York",
        "America/Phoenix",
        "America/Santiago",
        "America/Sao_Paulo",
        "America/Tijuana",
        "America/Toronto",
        "America/Vancouver",
        "Asia/Dubai",
        "Asia/Hong_Kong",
        "Asia/Jakarta",
        "Asia/Jerusalem",
        "Asia/Kolkata",
        "Asia/Seoul",
        "Asia/Shanghai",
        "Asia/Singapore",
        "Asia/Tokyo",
        "Australia/Perth",
        "Australia/Sydney",
        "Europe/Amsterdam",
        "Europe/Berlin",
        "Europe/London",
        "Europe/Madrid",
        "Europe/Paris",
        "Europe/Rome",
        "Pacific/Auckland",
        "Pacific/Honolulu"
    )

    fun monedas(
        monedaDispositivo: String,
        monedaConfigurada: String? = null
    ): List<CatalogoMonedaSistema> {
        val locale = Locale.forLanguageTag("es")
        val codes = (codigosMonedaComunes +
            monedaDispositivo.uppercase() +
            monedaConfigurada?.uppercase().orEmpty()).distinct()
        return codes.mapNotNull { code ->
            val currency = runCatching { Currency.getInstance(code) }.getOrNull() ?: return@mapNotNull null
            CatalogoMonedaSistema(
                codigo = currency.currencyCode,
                nombre = currency.getDisplayName(locale),
                simbolo = currency.getSymbol(locale)
            )
        }
    }

    fun zonasHorarias(
        zonaDispositivo: String,
        zonaConfigurada: String? = null
    ): List<CatalogoZonaHorariaSistema> {
        val ids = (zonasHorariasComunes + zonaDispositivo + zonaConfigurada.orEmpty()).distinct()
        return ids.mapNotNull { id ->
            val zone = runCatching { ZoneId.of(id) }.getOrNull() ?: return@mapNotNull null
            CatalogoZonaHorariaSistema(
                id = zone.id,
                nombre = nombreZona(zone.id)
            )
        }
    }

    private fun nombreZona(id: String): String {
        val nombres = mapOf(
            "UTC" to "Tiempo universal coordinado",
            "America/Anchorage" to "Anchorage, Alaska",
            "America/Argentina/Buenos_Aires" to "Buenos Aires, Argentina",
            "America/Bogota" to "Bogotá, Colombia",
            "America/Caracas" to "Caracas, Venezuela",
            "America/Chicago" to "Chicago, Estados Unidos",
            "America/Costa_Rica" to "Costa Rica",
            "America/Denver" to "Denver, Estados Unidos",
            "America/Guatemala" to "Guatemala",
            "America/Halifax" to "Halifax, Canadá",
            "America/Hermosillo" to "Hermosillo, México",
            "America/Lima" to "Lima, Perú",
            "America/Los_Angeles" to "Los Ángeles, Estados Unidos",
            "America/Mexico_City" to "Ciudad de México",
            "America/Monterrey" to "Monterrey, México",
            "America/New_York" to "Nueva York, Estados Unidos",
            "America/Phoenix" to "Phoenix, Estados Unidos",
            "America/Santiago" to "Santiago, Chile",
            "America/Sao_Paulo" to "São Paulo, Brasil",
            "America/Tijuana" to "Tijuana, México",
            "America/Toronto" to "Toronto, Canadá",
            "America/Vancouver" to "Vancouver, Canadá",
            "Asia/Dubai" to "Dubái, Emiratos Árabes Unidos",
            "Asia/Hong_Kong" to "Hong Kong",
            "Asia/Jakarta" to "Yakarta, Indonesia",
            "Asia/Jerusalem" to "Jerusalén, Israel",
            "Asia/Kolkata" to "Calcuta, India",
            "Asia/Seoul" to "Seúl, Corea del Sur",
            "Asia/Shanghai" to "Shanghái, China",
            "Asia/Singapore" to "Singapur",
            "Asia/Tokyo" to "Tokio, Japón",
            "Australia/Perth" to "Perth, Australia",
            "Australia/Sydney" to "Sídney, Australia",
            "Europe/Amsterdam" to "Ámsterdam, Países Bajos",
            "Europe/Berlin" to "Berlín, Alemania",
            "Europe/London" to "Londres, Reino Unido",
            "Europe/Madrid" to "Madrid, España",
            "Europe/Paris" to "París, Francia",
            "Europe/Rome" to "Roma, Italia",
            "Pacific/Auckland" to "Auckland, Nueva Zelanda",
            "Pacific/Honolulu" to "Honolulu, Hawái"
        )
        return nombres[id] ?: id.substringAfterLast('/').replace('_', ' ')
    }
}
