package com.smartsolutions.datwall.exceptions

class UnprocessableRequestException : Exception {

    constructor(): super()

    constructor(msg: String): super(msg)
}