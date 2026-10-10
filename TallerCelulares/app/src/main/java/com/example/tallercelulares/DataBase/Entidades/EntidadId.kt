package com.example.tallercelulares.DataBase.Entidades

import java.util.UUID

internal object EntidadId {
    fun nuevo(): String = UUID.randomUUID().toString()
}