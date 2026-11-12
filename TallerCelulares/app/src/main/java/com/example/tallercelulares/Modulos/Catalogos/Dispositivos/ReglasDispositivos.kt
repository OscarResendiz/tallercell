package com.example.tallercelulares.Modulos.Catalogos.Dispositivos

internal data class DatosDispositivoNormalizados(
    val tipo: String,
    val marca: String,
    val modelo: String,
    val color: String,
    val numeroSerie: String?,
    val imei: String?
)

internal object ReglasDispositivos {
    private const val MAX_TIPO = 64
    private const val MAX_MARCA = 100
    private const val MAX_MODELO = 120
    private const val MAX_COLOR = 80
    private const val MAX_IDENTIFICADOR = 128

    fun validar(
        tipo: String,
        marca: String,
        modelo: String,
        color: String,
        numeroSerie: String,
        imei: String
    ): Map<String, String> {
        val errores = mutableMapOf<String, String>()
        validarRequerido("tipo", tipo, MAX_TIPO, "El tipo es obligatorio.", errores)
        validarRequerido("marca", marca, MAX_MARCA, "La marca es obligatoria.", errores)
        validarRequerido("modelo", modelo, MAX_MODELO, "El modelo es obligatorio.", errores)
        validarOpcional("color", color, MAX_COLOR, "El color es demasiado largo.", errores)
        validarOpcional(
            "numeroSerie",
            numeroSerie,
            MAX_IDENTIFICADOR,
            "El número de serie es demasiado largo.",
            errores
        )
        validarOpcional("imei", imei, MAX_IDENTIFICADOR, "El IMEI es demasiado largo.", errores)
        return errores
    }

    fun normalizar(
        tipo: String,
        marca: String,
        modelo: String,
        color: String,
        numeroSerie: String,
        imei: String
    ) = DatosDispositivoNormalizados(
        tipo = tipo.trim(),
        marca = marca.trim(),
        modelo = modelo.trim(),
        color = color.trim(),
        numeroSerie = textoOpcional(numeroSerie),
        imei = textoOpcional(imei)
    )

    fun textoOpcional(valor: String): String? = valor.trim().takeIf(String::isNotEmpty)

    fun patronBusqueda(valor: String): String = valor.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    fun identificadorEnmascarado(valor: String?): String {
        val identificador = valor?.trim().orEmpty()
        if (identificador.isEmpty()) return "No registrado"
        return if (identificador.length <= 4) {
            "••••"
        } else {
            "••••••${identificador.takeLast(4)}"
        }
    }

    private fun validarRequerido(
        campo: String,
        valor: String,
        maximo: Int,
        mensajeVacio: String,
        errores: MutableMap<String, String>
    ) {
        val limpio = valor.trim()
        when {
            limpio.isEmpty() -> errores[campo] = mensajeVacio
            limpio.length > maximo -> errores[campo] = "Máximo $maximo caracteres."
        }
    }

    private fun validarOpcional(
        campo: String,
        valor: String,
        maximo: Int,
        mensajeLargo: String,
        errores: MutableMap<String, String>
    ) {
        if (valor.trim().length > maximo) errores[campo] = mensajeLargo
    }
}
