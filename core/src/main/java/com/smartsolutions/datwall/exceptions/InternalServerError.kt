package com.smartsolutions.datwall.exceptions

class InternalServerError: Exception {

    constructor(): super()

    constructor(msg: String): super(msg)
}