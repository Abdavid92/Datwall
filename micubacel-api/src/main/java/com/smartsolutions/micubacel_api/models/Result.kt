package com.smartsolutions.micubacel_api.models

abstract class Result<T> {

    class Success<T>(
        val value: T?
    ): Result<T>()

    class Fail<T>(
        val message: String,
        val th: Throwable?
    ): Result<T>()
}