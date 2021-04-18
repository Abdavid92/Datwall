package com.smartsolutions.datwall.exceptions

class UnauthorizedException : Exception {

    constructor(): super("Unauthorized")

    constructor(msg: String): super(msg)
}