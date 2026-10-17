package com.example.tallercelulares.DataBase.Entidades.CatalogoMonedaSistema

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CatalogoMonedaSistemaDao {
    @Query("SELECT * FROM catalogo_moneda_sistema WHERE activo = 1 ORDER BY nombre COLLATE NOCASE, codigo")
    suspend fun obtenerActivas(): List<CatalogoMonedaSistema>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExiste(moneda: CatalogoMonedaSistema)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarSiNoExisten(monedas: List<CatalogoMonedaSistema>)
}
