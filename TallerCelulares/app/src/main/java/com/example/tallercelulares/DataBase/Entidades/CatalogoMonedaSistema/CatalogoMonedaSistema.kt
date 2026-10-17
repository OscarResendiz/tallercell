package com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogo_moneda_sistema")
data class CatalogoMonedaSistema(
    @PrimaryKey val codigo: String,
    val nombre: String,
    val simbolo: String,
    val activo: Boolean = true
)
