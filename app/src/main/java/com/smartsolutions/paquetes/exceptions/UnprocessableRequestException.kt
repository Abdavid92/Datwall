package com.smartsolutions.paquetes.exceptions

import java.lang.Exception

class UnprocessableRequestException : Exception {

    constructor() : super()

    constructor(message: String?) : super(message)

}