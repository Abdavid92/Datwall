package com.smartsolutions.datwall.webApis.models

abstract class Result<T> {

    class Success<T>(
        val value: T?
    ): Result<T>()

    class Fail<T>(
        val message: String
    ): Result<T>()
}