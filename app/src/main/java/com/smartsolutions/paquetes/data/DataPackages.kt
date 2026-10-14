package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.data.DataPackages.PackageId.*
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.*
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_NONE

object DataPackages {

    /**
     * v3  Cambiado el precio del Plan Basico a 110
     * v4  Oferta de octubre de 2026: Plan Diario y planes de 35 días. Los paquetes anteriores
     *     quedan obsoletos.
     **/
    const val PACKAGES_VERSION = 4

    const val MESSAGING_BAG_KEY = "bolsa Mensajeria"

    const val GENERAL_DURATION = 35

    /**
     * Código de compra del Plan Diario.
     * */
    const val DAILY_PLAN_USSD = "*133*1*3#"

    /**
     * Prefijo del código de compra de los planes de 35 días. Se completa con el
     * índice del plan en el menú *133*1*4# y #.
     * */
    const val PLANS_USSD_PREFIX = "*133*1*4*"

    private const val PLAN_DESCRIPTION_SUFFIX = "Tiene una vigencia de 35 días a partir del primer " +
            "uso. Si compra otro plan estando este activo, los recursos se suman y se extiende la vigencia."

    val PACKAGES = arrayOf(
        DataPackage(
            DailyBag,
            "Plan Diario",
            "Proporciona 200MB para todas las redes a consumir en 24 horas a partir del primer uso.",
            25f,
            DataValue(200.0, DataUnit.MB).toBytes(),
            0,
            0,
            NETWORK_3G_4G,
            -1,
            1,
            "Plan Diario 200MB"
        ),
        DataPackage(
            Plan_4_5GB,
            "Plan 4.5 GB",
            "Proporciona 4.5GB para todas las redes y 300MB para navegación nacional. " +
                    PLAN_DESCRIPTION_SUFFIX,
            240f,
            DataValue(4.5, DataUnit.GB).toBytes(),
            0,
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            1,
            GENERAL_DURATION,
            "Plan 4.5GB"
        ),
        DataPackage(
            Plan_2GB,
            "Plan 120",
            "Proporciona 2GB para todas las redes, 300MB para navegación nacional, " +
                    "15min para llamadas y 20sms. " + PLAN_DESCRIPTION_SUFFIX,
            120f,
            DataValue(2.0, DataUnit.GB).toBytes(),
            0,
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            2,
            GENERAL_DURATION,
            "Plan 2GB+15MIN+20SMS",
            minutes = 15,
            sms = 20
        ),
        DataPackage(
            Plan_4GB,
            "Plan 240",
            "Proporciona 4GB para todas las redes, 300MB para navegación nacional, " +
                    "35min para llamadas y 40sms. " + PLAN_DESCRIPTION_SUFFIX,
            240f,
            DataValue(4.0, DataUnit.GB).toBytes(),
            0,
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            3,
            GENERAL_DURATION,
            "Plan 4GB+35MIN+40SMS",
            minutes = 35,
            sms = 40
        ),
        DataPackage(
            Plan_6GB,
            "Plan 360",
            "Proporciona 6GB para todas las redes, 300MB para navegación nacional, " +
                    "60min para llamadas y 70sms. " + PLAN_DESCRIPTION_SUFFIX,
            360f,
            DataValue(6.0, DataUnit.GB).toBytes(),
            0,
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            4,
            GENERAL_DURATION,
            "Plan 6GB+60MIN+70SMS",
            minutes = 60,
            sms = 70
        ),

        // Paquetes obsoletos: se conservan para no perder el historial de compras.
        DataPackage(
            Combined_Basic,
            "Plan Básico",
            "Proporciona 600MB disponibles para todas las redes y 800MB solo para LTE. " +
                    "Ademas contará con 15min para llamadas y 20sms. La vigencia es de 30 dias " +
                    "a partir del primer uso de cualquiera de los tres recursos.",
            110f,
            DataValue(600.0, DataUnit.MB).toBytes(),
            DataValue(800.0, DataUnit.MB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            1,
            30,
            "600MB +800MB LTE",
            deprecated = true,
            minutes = 15,
            sms = 20
        ),
        DataPackage(
            Combined_Medium,
            "Plan Medio",
            "Proporciona 1.5GB disponibles para todas las redes y 2GB solo para LTE. " +
                    "Ademas contará con 35min para llamadas y 40sms. La vigencia es de 30 dias " +
                    "a partir del primer uso de cualquiera de los tres recursos.",
            250f,
            DataValue(1.5, DataUnit.GB).toBytes(),
            DataValue(2.0, DataUnit.GB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            2,
            30,
            "1.5GB +2GB LTE",
            deprecated = true,
            minutes = 35,
            sms = 40
        ),
        DataPackage(
            Combined_Extra,
            "Plan Extra",
            "Proporciona 3.5GB disponibles para todas las redes y 4.5GB solo para LTE. " +
                    "Ademas contará con 75min para llamadas y 80sms. La vigencia es de 30 dias " +
                    "a partir del primer uso de cualquiera de los tres recursos.",
            500f,
            DataValue(3.5, DataUnit.GB).toBytes(),
            DataValue(4.5, DataUnit.GB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_3G_4G,
            3,
            30,
            "3.5GB +4.5GB LTE",
            deprecated = true,
            minutes = 75,
            sms = 80
        ),
        DataPackage(
            P_1GB_LTE,
            "Paquete 1 GB LTE",
            "Proporciona 1GB disponible solo para la red LTE. " +
                    "Tiene una vigencia de 30 dias a partir del primer uso.",
            100f,
            DataValue(0.0, DataUnit.B).toBytes(),
            DataValue(1.0, DataUnit.GB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_4G,
            1,
            30,
            "1GB solo LTE",
            deprecated = true
        ),
        DataPackage(
            P_2_5GB_LTE,
            "Paquete 2.5 GB LTE",
            "Proporciona 2.5GB disponible solo para la red LTE. " +
                    "Tiene una vigencia de 30 dias a partir del primer uso.",
            200f,
            DataValue(0.0, DataUnit.B).toBytes(),
            DataValue(2.5, DataUnit.GB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_4G,
            2,
            30,
            "2.5GB solo LTE",
            deprecated = true
        ),
        DataPackage(
            P_4GB_12GB_LTE,
            "Paquete 16 GB",
            "Proporciona 4GB para todas las redes más 12GB disponible solo para la red LTE. " +
                    "Tiene una vigencia de 30 dias a partir del primer uso.",
            950f,
            DataValue(4.0, DataUnit.GB).toBytes(),
            DataValue(12.0, DataUnit.GB).toBytes(),
            DataValue(300.0, DataUnit.MB).toBytes(),
            NETWORK_4G,
            3,
            30,
            "12GB LTE",
            deprecated = true
        ),
        DataPackage(
            MessagingBag,
            "Bolsa Mensajería",
            "Proporciona 600 MB para consumir solamente usando el correo Nauta o Todus. Tiene una " +
                    "vigencia de 30 días a partir del primer uso",
            25f,
            DataValue(600.0, DataUnit.MB).toBytes(),
            0L,
            0L,
            NETWORK_NONE,
            2,
            30,
            MESSAGING_BAG_KEY,
            deprecated = true
        )
    )

    /**
     * Construye el código ussd de compra de un plan según el menú *133#.
     * */
    fun buildPurchaseUssd(dataPackage: DataPackage): String =
        if (dataPackage.id == DailyBag)
            DAILY_PLAN_USSD
        else
            "$PLANS_USSD_PREFIX${dataPackage.index}#"

    /**
     * Busca el paquete de un SMS de compra comparando su [DataPackage.smsKey] sin espacios
     * ni mayúsculas. Incluye los obsoletos, para leer SMS antiguos. Si coinciden varias
     * claves gana la más larga, para no confundir planes cuyas claves empiezan igual.
     * */
    fun findPurchasedPackage(smsBody: String): DataPackage? {
        val body = normalize(smsBody)
        return PACKAGES
            .filter { body.contains(normalize(it.smsKey)) }
            .maxByOrNull { normalize(it.smsKey).length }
    }

    /**
     * Remitentes de los SMS del operador: "cubacel" (antes) y "ETECSA" (desde 2026).
     * */
    fun isOperatorSender(sender: String): Boolean =
        OPERATOR_SENDERS.any { it.equals(sender.trim(), true) }

    private val OPERATOR_SENDERS = listOf("cubacel", "ETECSA")

    private fun normalize(text: String) = text.filterNot { it.isWhitespace() }.lowercase()

    /**
     * Se guarda en la base de datos por ordinal: los valores nuevos se añaden siempre
     * al final y los existentes no se mueven ni se borran.
     * */
    enum class PackageId {
        DailyBag,
        Combined_Basic,
        Combined_Medium,
        Combined_Extra,
        P_1GB_LTE,
        P_2_5GB_LTE,
        P_4GB_12GB_LTE,
        MessagingBag,
        Plan_4_5GB,
        Plan_2GB,
        Plan_4GB,
        Plan_6GB
    }
}
