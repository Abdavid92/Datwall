package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.helpers.DataUnit
import com.smartsolutions.paquetes.helpers.DataValue
import com.smartsolutions.paquetes.repositories.models.DataPackage

object DataPackagesContract {

    object DailyBag {
        const val name = "Bolsa Diaria LTE"
        const val description = "Navega en la red LTE por 24 horas."
        const val price = 25f
        const val bytes = 0L
        val bytesLte = DataValue(200.0, DataUnit.MB)
        const val bonusBytes = 0L
        const val bonusCuBytes = 0L
        const val network = DataPackage.NETWORK_4G
        const val index = -1
    }

    object P_1GbLte {
        const val name = "Paquete 1 GB LTE"
        const val description = "Este paquete consta de 1 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias."
        const val price = 100f
        const val bytes = 0L
        val bytesLte = DataValue(1.0, DataUnit.GB)
        const val bonusBytes = 0L
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_4G
        const val index = 1
    }

    object P_2_5GbLte {
        const val name = "Paquete 2.5 GB LTE"
        const val description = "Este paquete consta de 2.5 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias."
        const val price = 200f
        const val bytes = 0L
        val bytesLte = DataValue(2.5, DataUnit.GB)
        const val bonusBytes = 0L
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_4G
        const val index = 2
    }

    object P_14GbLte {
        const val name = "Paquete 14 GB"
        const val description = "Este paquete consta de 10 GB que solo podrá utilizar bajo la red 4G(LTE) y " +
                "4 GB que podrá usar en todas las redes. Tiene un vigencia de 30 dias."
        const val price = 1125f
        val bytes = DataValue(4.0, DataUnit.GB)
        val bytesLte = DataValue(10.0, DataUnit.GB)
        const val bonusBytes = 0L
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_4G
        const val index = 3
    }

    object P_400Mb {
        const val name = "Paquete 400 MB"
        const val description = "Este paquete consta de 400 MB que podrá usar en todas las redes y" +
                " un bono de 500 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        const val price = 125f
        val bytes = DataValue(400.0, DataUnit.MB)
        const val bytesLte = 0L
        val bonusBytes = DataValue(500.0, DataUnit.MB)
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_3G_4G
        const val index = 1
    }

    object P_600Mb {
        const val name = "Paquete 600 MB"
        const val description = "Este paquete consta de 600 MB que podrá usar en todas las redes y" +
                " un bono de 800 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        const val price = 175f
        val bytes = DataValue(600.0, DataUnit.MB)
        const val bytesLte = 0L
        val bonusBytes = DataValue(800.0, DataUnit.MB)
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_3G_4G
        const val index = 2
    }

    object P_1Gb {
        const val name = "Paquete 1 GB"
        const val description = "Este paquete consta de 1 GB que podrá usar en todas las redes y" +
                " un bono de 1.5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        const val price = 250f
        val bytes = DataValue(1.0, DataUnit.GB)
        const val bytesLte = 0L
        val bonusBytes = DataValue(1.5, DataUnit.GB)
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_3G_4G
        const val index = 3
    }

    object P_2_5Gb {
        const val name = "Paquete 2.5 GB"
        const val description = "Este paquete consta de 2.5 GB que podrá usar en todas las redes y" +
                " un bono de 3 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        const val price = 500f
        val bytes = DataValue(2.5, DataUnit.GB)
        const val bytesLte = 0L
        val bonusBytes = DataValue(3.0, DataUnit.GB)
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_3G_4G
        const val index = 4
    }

    object P_4Gb {
        const val name = "Paquete 4 GB"
        const val description = "Este paquete consta de 4 GB que podrá usar en todas las redes y" +
                " un bono de 5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias."
        const val price = 750f
        val bytes = DataValue(4.0, DataUnit.GB)
        const val bytesLte = 0L
        val bonusBytes = DataValue(5.0, DataUnit.GB)
        val bonusCuBytes = DataValue(300.0, DataUnit.MB)
        const val network = DataPackage.NETWORK_3G_4G
        const val index = 5
    }
}