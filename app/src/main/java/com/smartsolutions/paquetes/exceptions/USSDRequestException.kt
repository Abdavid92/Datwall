package com.smartsolutions.paquetes.exceptions

/**
 * Excepción que se lanza cuando hubo un error al ejecutar
 * un código ussd.
 * */
class USSDRequestException(
    /**
     * Código del error (constantes de `USSDHelper`). La UI lo traduce a texto.
     * */
    val errorCode: Int
): Exception("USSD error $errorCode")