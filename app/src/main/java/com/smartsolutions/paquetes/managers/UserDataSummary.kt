package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.DataFormatter
import com.smartsolutions.paquetes.managers.models.UserDataOrder
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.util.concurrent.TimeUnit

/**
 * Totales de los datos de una línea, en bytes. No cuentan los datos nacionales ni la bolsa
 * de mensajería.
 * */
data class UserDataSummary(
    val totalBytes: Long,
    val restBytes: Long,
    val usedBytes: Long,
    /**
     * Porcentaje restante.
     * */
    val percent: Int
) {

    /**
     * Valores de un solo dato: gastado, restante, porcentaje restante y tiempo hasta que vence.
     * */
    data class Item(
        val usedBytes: Long,
        val restBytes: Long,
        val percent: Int,
        val timeLeft: Pair<Int, TimeUnit>
    )

    companion object {

        fun of(data: List<UserDataBytes>): UserDataSummary {
            val counted = data.filter { it.type != DataType.National && it.type != DataType.MessagingBag }

            val total = counted.sumOf { it.initialBytes }
            val rest = counted.sumOf { it.bytes }
            val used = counted.sumOf { it.initialBytes - it.bytes }

            return UserDataSummary(total, rest, used, DataFormatter.percent(total, rest))
        }

        fun item(data: UserDataBytes, now: Long = System.currentTimeMillis()) = Item(
            usedBytes = data.initialBytes - data.bytes,
            restBytes = data.bytes,
            percent = DataFormatter.percent(data.initialBytes, data.bytes),
            timeLeft = DataFormatter.timeLeft(now, data.expiredTime)
        )

        /**
         * Datos que se muestran: los que existen y no han vencido, en el orden elegido.
         * */
        fun visible(data: List<UserDataBytes>, order: UserDataOrder): List<UserDataBytes> =
            when (order) {
                UserDataOrder.SIZE_ASC -> data.sortedBy { it.bytes }
                UserDataOrder.SIZE_DESC -> data.sortedByDescending { it.bytes }
                UserDataOrder.EXPIRE_ASC -> data.sortedBy { it.expiredTime }
                UserDataOrder.EXPIRE_DESC -> data.sortedByDescending { it.expiredTime }
                UserDataOrder.NORMAL -> data
            }.filter { it.exists() && !it.isExpired() }
    }
}
