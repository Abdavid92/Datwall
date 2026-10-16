package com.smartsolutions.paquetes.exceptions

import java.io.Serializable

class MissingPermissionException: Exception, Serializable {

    val permission: Array<String>

    constructor(permission: String): super() {
        this.permission = arrayOf(permission)
    }

    constructor(permission: Array<String>): super() {
        this.permission = permission
    }

    constructor(permission: String, message: String): super(message) {
        this.permission = arrayOf(permission)
    }

    constructor(permission: Array<String>, message: String): super(message) {
        this.permission = permission
    }
}
