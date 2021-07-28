package com.smartsolutions.paquetes.serverApis.models

abstract class Result<T> {

    val isSuccess: Boolean get() = this !is Failure<*>

    val isFailure: Boolean get() = this is Failure<*>

    fun getOrNull(): T? {
        if (isSuccess)
            return (this as Success<T>).value
        return null
    }

    fun getOrThrow(): T {
        if (isSuccess)
            return (this as Success<T>).value
        else
            throw (this as Failure<T>).throwable
    }

    fun getThrowableOrNull(): Throwable? {
        if (isFailure) {
            return (this as Failure).throwable
        }
        return null
    }

    class Success<T>(
        val value: T
    ) : Result<T>()

    class Failure<T>(
        val throwable: Throwable
    ) : Result<T>() {
        override fun equals(other: Any?): Boolean = other is Failure<*> && throwable == other.throwable
        override fun hashCode(): Int = throwable.hashCode()
        override fun toString(): String = "Failure($throwable)"
    }
}