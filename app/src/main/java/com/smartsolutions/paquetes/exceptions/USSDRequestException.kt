package com.smartsolutions.paquetes.exceptions

/**
 * Excepción que se lanza cuando hubo un error al ejecutar
 * un código ussd.
 * */
class USSDRequestException(
    val errorCode: Int,
    message: String
): Exception(message)