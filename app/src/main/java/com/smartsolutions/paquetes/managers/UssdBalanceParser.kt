package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataUnit
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataValue
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Lee los saldos de datos de las respuestas ussd de ETECSA. Los formatos están
 * en specs/002-actualizar-planes-etecsa/contracts/ussd-sms.md.
 *
 * Un vencimiento 0 significa que los datos aún no tienen fecha (se activan con el
 * primer uso o la línea no tiene vigencia).
 * */
object UssdBalanceParser {

    private const val PAQUETES = "Paquetes:"
    private const val DATOS = "Datos:"
    private const val DIARIA = "Diaria:"
    private const val MENSAJERIA = "Mensajeria:"
    private const val NATIONAL = "Datos.cu"
    private const val PLUS_SYMBOL = "+"
    private const val LTE = "LTE"

    private val PACKAGE_KEYS = listOf(PAQUETES, DATOS, DIARIA, MENSAJERIA)
    private val BONUS_KEYS = listOf(DATOS, NATIONAL)

    private val AMOUNT = Regex("""^\s*(\d+(?:\.\d+)?)\s*([KMG]?B)""", RegexOption.IGNORE_CASE)
    private val VALID_FOR = Regex("""validos\s+(\d+)\s+(dias|horas)""", RegexOption.IGNORE_CASE)
    private val EXPIRES_ON = Regex("""vence\s+(\d{2}-\d{2}-\d{2})""", RegexOption.IGNORE_CASE)

    /**
     * Respuesta de *222*328# o de *111# (líneas empresariales).
     * */
    fun parsePackages(response: String, now: Long = System.currentTimeMillis()): List<DataBytes> {
        val data = mutableListOf<DataBytes>()

        if (response.contains(PAQUETES)) {
            data.addAll(legacyInternationals(response, now))
        } else {
            bytesAfter(DATOS, response)?.let {
                data.add(DataBytes(DataType.International, it, validUntil(DATOS, response, now)))
            }
        }

        bytesAfter(DIARIA, response)?.takeIf { it > 0 }?.let {
            data.add(DataBytes(DataType.DailyBag, it, validUntil(DIARIA, response, now)))
        }

        bytesAfter(MENSAJERIA, response)?.takeIf { it > 0 }?.let {
            data.add(DataBytes(DataType.MessagingBag, it, validUntil(MENSAJERIA, response, now)))
        }

        return data
    }

    /**
     * Respuesta de *111# de una línea empresarial. La cuota se reinicia al principio
     * de cada mes, así que los datos vencen el último día del mes en curso.
     * */
    fun parseBusinessBalance(response: String, now: Long = System.currentTimeMillis()): List<DataBytes> {
        val endOfMonth = Calendar.getInstance().run {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            setEndOfDay()
            timeInMillis
        }

        return parsePackages(response, now).map {
            if (it.type == DataType.International && it.expiredTime == 0L)
                DataBytes(it.type, it.bytes, endOfMonth)
            else
                it
        }
    }

    /**
     * Respuesta de *222*266# (bonos y datos .cu).
     * */
    fun parseBonus(response: String): List<DataBytes> {
        val data = mutableListOf<DataBytes>()

        bytesAfter(DATOS, response, BONUS_KEYS)?.let {
            data.add(DataBytes(DataType.PromoBonus, it, expiresOn(DATOS, response)))
        }

        bytesAfter(NATIONAL, response, BONUS_KEYS)?.let {
            data.add(DataBytes(DataType.National, it, expiresOn(NATIONAL, response)))
        }

        if (response.contains(PLUS_SYMBOL) && response.contains(LTE)) {
            bytesAfter(PLUS_SYMBOL, response, BONUS_KEYS)?.let {
                data.add(DataBytes(DataType.PromoBonusLte, it, expiresOn(DATOS, response)))
            }
        }

        return data
    }

    /**
     * Formato anterior a 2026: "Paquetes: X [+ Y LTE] validos N dias".
     * */
    private fun legacyInternationals(text: String, now: Long): List<DataBytes> {
        val packages = bytesAfter(PAQUETES, text) ?: 0L
        val expiration = validUntil(PAQUETES, text, now)

        val (international, internationalLte) = when {
            !text.contains(LTE) -> packages to 0L
            !text.contains(PLUS_SYMBOL) -> 0L to packages
            else -> packages to (bytesAfter(PLUS_SYMBOL, text) ?: 0L)
        }

        return listOf(
            DataBytes(DataType.International, international, expiration),
            DataBytes(DataType.InternationalLte, internationalLte, expiration)
        )
    }

    /**
     * Texto desde [key] hasta la siguiente clave conocida, para no mezclar
     * cantidades y vigencias de recursos distintos.
     * */
    private fun segment(key: String, text: String, keys: List<String>): String? {
        val start = text.indexOf(key)
        if (start < 0)
            return null

        val from = start + key.length
        val end = keys.filter { it != key }
            .map { text.indexOf(it, from) }
            .filter { it >= 0 }
            .minOrNull() ?: text.length

        return text.substring(from, end)
    }

    private fun bytesAfter(key: String, text: String, keys: List<String> = PACKAGE_KEYS): Long? {
        val match = segment(key, text, keys)?.let { AMOUNT.find(it) } ?: return null

        val unit = when (match.groupValues[2].uppercase()) {
            "KB" -> DataUnit.KB
            "MB" -> DataUnit.MB
            "GB" -> DataUnit.GB
            else -> DataUnit.B
        }

        return DataValue(match.groupValues[1].toDouble(), unit).toBytes()
    }

    private fun validUntil(key: String, text: String, now: Long): Long {
        val match = segment(key, text, PACKAGE_KEYS)?.let { VALID_FOR.find(it) } ?: return 0L
        val amount = match.groupValues[1].toInt()

        return Calendar.getInstance().run {
            timeInMillis = now
            if (match.groupValues[2].equals("horas", true)) {
                add(Calendar.HOUR_OF_DAY, amount)
            } else {
                add(Calendar.DAY_OF_MONTH, amount)
                setEndOfDay()
            }
            timeInMillis
        }
    }

    private fun expiresOn(key: String, text: String): Long {
        val match = segment(key, text, BONUS_KEYS)?.let { EXPIRES_ON.find(it) } ?: return 0L
        val date = try {
            SimpleDateFormat("dd-MM-yy", Locale.US).parse(match.groupValues[1])
        } catch (e: Exception) {
            null
        } ?: return 0L

        return Calendar.getInstance().run {
            time = date
            setEndOfDay()
            timeInMillis
        }
    }

    private fun Calendar.setEndOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 0)
    }
}
