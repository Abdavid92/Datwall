package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.managers.models.DataBytes.*

object DataPackagesContract {

    const val PROMO_BONUS_KEY = "Bonos: 1024 MB"

    val DailyBag = PackageModel (
        "Bolsa Diaria LTE",
        "Navega en la red LTE por 24 horas.",
        25f,
        DataValue(0.0, DataUnit.KB),
        DataValue(200.0, DataUnit.MB),
        DataValue(0.0, DataUnit.KB),
        DataValue(0.0, DataUnit.KB),
        NETWORK_4G,
        -1,
        "bolsa Diaria de 200MB")

    val P_1GbLte = PackageModel (
        "Paquete 1 GB LTE",
        "Este paquete consta de 1 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias.",
        100f,
        DataValue(0.0, DataUnit.KB),
        DataValue(1.0, DataUnit.GB),
        DataValue(0.0, DataUnit.KB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_4G,
        1,
        "1GB solo LTE")

    val P_2_5GbLte = PackageModel (
        "Paquete 2.5 GB LTE",
        "Este paquete consta de 2.5 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias.",
        200f,
        DataValue(0.0, DataUnit.KB),
        DataValue(2.5, DataUnit.GB),
        DataValue(0.0, DataUnit.KB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_4G,
        2,
       "2.5GB solo LTE")

    val P_14GbLte = PackageModel (
        "Paquete 14 GB",
        "Este paquete consta de 10 GB que solo podrá utilizar bajo la red 4G(LTE) y " +
                "4 GB que podrá usar en todas las redes. Tiene un vigencia de 30 dias.",
        1125f,
       DataValue(4.0, DataUnit.GB),
        DataValue(10.0, DataUnit.GB),
        DataValue(0.0, DataUnit.KB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_4G,
       3,
        "14GB solo LTE")


    val P_400Mb = PackageModel (
        "Paquete 400 MB",
        "Este paquete consta de 400 MB que podrá usar en todas las redes y" +
                " un bono de 500 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
        125f,
        DataValue(400.0, DataUnit.MB),
        DataValue(0.0, DataUnit.KB),
        DataValue(500.0, DataUnit.MB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_3G_4G,
        1,
        "400MB + Bono LTE")


    val P_600Mb = PackageModel (
        "Paquete 600 MB",
        "Este paquete consta de 600 MB que podrá usar en todas las redes y" +
                " un bono de 800 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
        175f,
        DataValue(600.0, DataUnit.MB),
        DataValue(0.0, DataUnit.KB),
        DataValue(800.0, DataUnit.MB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_3G_4G,
        2,
        "600MB + Bono LTE")


    val P_1Gb = PackageModel (
       "Paquete 1 GB",
        "Este paquete consta de 1 GB que podrá usar en todas las redes y" +
                " un bono de 1.5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
        250f,
        DataValue(1.0, DataUnit.GB),
        DataValue(0.0, DataUnit.KB),
        DataValue(1.5, DataUnit.GB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_3G_4G,
        3,
        "1GB + Bono LTE")


   val P_2_5Gb = PackageModel (
       "Paquete 2.5 GB",
        "Este paquete consta de 2.5 GB que podrá usar en todas las redes y" +
                " un bono de 3 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
        500f,
       DataValue(2.5, DataUnit.GB),
       DataValue(0.0, DataUnit.KB),
       DataValue(3.0, DataUnit.GB),
       DataValue(300.0, DataUnit.MB),
       NETWORK_3G_4G,
       4,
       "2.5GB + Bono LTE")

    val P_4Gb = PackageModel (
         "Paquete 4 GB",
        "Este paquete consta de 4 GB que podrá usar en todas las redes y" +
                " un bono de 5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
        750f,
        DataValue(4.0, DataUnit.GB),
        DataValue(0.0, DataUnit.KB),
        DataValue(5.0, DataUnit.GB),
        DataValue(300.0, DataUnit.MB),
        NETWORK_3G_4G,
        5,
        "4GB + Bono LTE")

    val PackagesList = listOf(
        DailyBag,
        P_1GbLte,
        P_2_5GbLte,
        P_14GbLte,
        P_400Mb,
        P_600Mb,
        P_1Gb,
        P_2_5Gb,
        P_4Gb
    )

    data class PackageModel (
        val name: String,
        val description: String,
        val price: Float,
        val bytes: DataValue,
        val bytesLte: DataValue,
        val bonusBytes: DataValue,
        val bonusCuBytes: DataValue,
        val network: String,
        val index: Int,
        val smsKey: String) {

        val id: String
            get() = createDataPackageId(name, price)

    }
}