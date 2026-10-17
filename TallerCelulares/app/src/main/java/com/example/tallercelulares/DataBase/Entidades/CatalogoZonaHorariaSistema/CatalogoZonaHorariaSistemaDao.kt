package com.example.tallercelulares.DataBase.Entidades.CatalogoZonaHorariaSistema

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogoZonaHorariaSistemaDao {
    @Query("SELECT * FROM catalogo_zona_horaria_sistema WHERE activo = 1 ORDER BY nombre COLLATE NOCASE, id")
    suspend fun obtenerActivas(): List<CatalogoZonaHorariaSistema>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(zonaHoraria: CatalogoZonaHorariaSistema)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExisten(zonasHorarias: List<CatalogoZonaHorariaSistema>)
}
