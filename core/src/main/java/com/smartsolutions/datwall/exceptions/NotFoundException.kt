package com.smartsolutions.datwall.exceptions

class NotFoundException: Exception {

    constructor(): super("Not found")

    constructor(msg: String): super(msg)
}