package com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogo_zona_horaria_sistema")
data class CatalogoZonaHorariaSistema(
    @PrimaryKey val id: String,
    val nombre: String,
    val activo: Boolean = true
)
