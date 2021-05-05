package com.smartsolutions.paquetes.exceptions

import java.lang.Exception

class UnprocessableRequestException : Exception {

    var reason: Reason? = null

    constructor() : super()

    constructor(message: String?) : super(message)

    constructor(reason: Reason): super() {
        this.reason = reason
    }

    constructor(reason: Reason, message: String?): super(message) {
        this.reason = reason
    }

    enum class Reason {
        NO_LOGIN,
        BAG_CREDENTIALS,
        WRONG_CODE,
        MISSING_PERMISSION
    }
}