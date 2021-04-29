package com.smartsolutions.paquetes.exceptions

/**
 * Exceción que se lanza cuando hubo un error al ejecutar
 * un código ussd.
 * */
class USSDRequestException(
    val errorCode: Int,
    message: String
): Exception(message)