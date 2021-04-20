package com.smartsolutions.micubacel_client.exceptions

import java.lang.Exception

class UnprocessableRequestException : Exception {

    constructor() : super()

    constructor(message: String?) : super(message)

}