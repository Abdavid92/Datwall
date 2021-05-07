package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.managers.models.DataBytes.*

object DataPackagesContract {

    object DailyBag: PackageModel {
        override val name = "Bolsa Diaria LTE"
        override val description = "Navega en la red LTE por 24 horas."
        override val price = 25f
        override val bytes = DataValue(0.0, DataUnit.KB)
        override val bytesLte = DataValue(200.0, DataUnit.MB)
        override val bonusBytes = DataValue(0.0, DataUnit.KB)
        override val bonusCuBytes = DataValue(0.0, DataUnit.KB)
        override val network = DataPackage.NETWORK_4G
        override val index = -1
        override val smsKey = "bolsa Diaria de 200MB"
    }

    object P_1GbLte: PackageModel {
        override val name = "Paquete 1 GB LTE"
        override val description = "Este paquete consta de 1 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias."
        override val price = 100f
        override val bytes = DataValue(0.0, DataUnit.KB)
        override val bytesLte = DataValue(1.0, DataUnit.GB)
        override val bonusBytes = DataValue(0.0, DataUnit.KB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_4G
        override val index = 1
        override val smsKey = "1GB solo LTE"
    }

    object P_2_5GbLte: PackageModel {
        override val name = "Paquete 2.5 GB LTE"
        override val description = "Este paquete consta de 2.5 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias."
        override val price = 200f
        override val bytes = DataValue(0.0, DataUnit.KB)
        override val bytesLte = DataValue(2.5, DataUnit.GB)
        override val bonusBytes = DataValue(0.0, DataUnit.KB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_4G
        override val index = 2
        override val smsKey = "2.5GB solo LTE"
    }

    object P_14GbLte: PackageModel {
        override val name = "Paquete 14 GB"
        override val description = "Este paquete consta de 10 GB que solo podrá utilizar bajo la red 4G(LTE) y " +
                "4 GB que podrá usar en todas las redes. Tiene un vigencia de 30 dias."
        override val price = 1125f
        override val bytes = DataValue(4.0, DataUnit.GB)
        override val bytesLte = DataValue(10.0, DataUnit.GB)
        override val bonusBytes = DataValue(0.0, DataUnit.KB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_4G
        override val index = 3
        override val smsKey = "14GB solo LTE"
    }

    object P_400Mb: PackageModel {
        override val name = "Paquete 400 MB"
        override val description = "Este paquete consta de 400 MB que podrá usar en todas las redes y" +
                " un bono de 500 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        override val price = 125f
        override val bytes = DataValue(400.0, DataUnit.MB)
        override val bytesLte = DataValue(0.0, DataUnit.KB)
        override val bonusBytes = DataValue(500.0, DataUnit.MB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_3G_4G
        override val index = 1
        override val smsKey = "400MB + Bono LTE"
    }

    object P_600Mb: PackageModel {
        override val name = "Paquete 600 MB"
        override val description = "Este paquete consta de 600 MB que podrá usar en todas las redes y" +
                " un bono de 800 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        override val price = 175f
        override val bytes = DataValue(600.0, DataUnit.MB)
        override val bytesLte = DataValue(0.0, DataUnit.KB)
        override val bonusBytes = DataValue(800.0, DataUnit.MB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_3G_4G
        override val index = 2
        override val smsKey = "600MB + Bono LTE"
    }

    object P_1Gb: PackageModel {
        override val name = "Paquete 1 GB"
        override val description = "Este paquete consta de 1 GB que podrá usar en todas las redes y" +
                " un bono de 1.5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        override val price = 250f
        override val bytes = DataValue(1.0, DataUnit.GB)
        override val bytesLte = DataValue(0.0, DataUnit.KB)
        override val bonusBytes = DataValue(1.5, DataUnit.GB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_3G_4G
        override val index = 3
        override val smsKey = "1GB + Bono LTE"
    }

    object P_2_5Gb: PackageModel {
        override val name = "Paquete 2.5 GB"
        override val description = "Este paquete consta de 2.5 GB que podrá usar en todas las redes y" +
                " un bono de 3 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        override val price = 500f
        override val bytes = DataValue(2.5, DataUnit.GB)
        override val bytesLte = DataValue(0.0, DataUnit.KB)
        override val bonusBytes = DataValue(3.0, DataUnit.GB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_3G_4G
        override val index = 4
        override val smsKey = "2.5GB + Bono LTE"
    }

    object P_4Gb: PackageModel {
        override val name = "Paquete 4 GB"
        override val description = "Este paquete consta de 4 GB que podrá usar en todas las redes y" +
                " un bono de 5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        override val price = 750f
        override val bytes = DataValue(4.0, DataUnit.GB)
        override val bytesLte = DataValue(0.0, DataUnit.KB)
        override val bonusBytes = DataValue(5.0, DataUnit.GB)
        override val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        override val network = DataPackage.NETWORK_3G_4G
        override val index = 5
        override val smsKey = "4GB + Bono LTE"
    }

    internal interface PackageModel {
        val name: String
        val description: String
        val price: Float
        val bytes: DataValue
        val bytesLte: DataValue
        val bonusBytes: DataValue
        val bonusCuBytes: DataValue
        val network: String
        val index: Int
        val smsKey: String
    }
}