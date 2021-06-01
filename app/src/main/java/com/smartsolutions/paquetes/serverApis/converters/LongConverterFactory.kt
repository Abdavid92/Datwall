package com.smartsolutions.paquetes.serverApis.converters

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.*

class LongConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, Long>? {
        return LongResponseBodyConverter()
    }
}

class LongResponseBodyConverter : Converter<ResponseBody, Long> {

    override fun convert(body: ResponseBody): Long? {
        return try {
            String(body.bytes()).toLong()
        } catch (e: Exception) {
            Date().time
        }
    }
}