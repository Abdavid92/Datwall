package com.smartsolutions.paquetes.managers.models

/**
 * Orden de los datos del usuario en Resumen. Se guarda por nombre en las preferencias y la
 * pantalla usa la posición de cada valor: no cambiar nombres ni orden.
 * */
enum class UserDataOrder {
    SIZE_DESC,
    SIZE_ASC,
    EXPIRE_ASC,
    EXPIRE_DESC,
    NORMAL
}
