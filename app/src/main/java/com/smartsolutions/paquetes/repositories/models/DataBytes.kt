package com.smartsolutions.paquetes.repositories.models

import androidx.room.ColumnInfo

open class DataBytes(
    @ColumnInfo
    val type: DataType,
    @ColumnInfo
    var bytes: Long,
    @ColumnInfo(name = "expired_time")
    var expiredTime: Long
) {

    enum class DataType {
        International, // Datos de consumo internacional en todas las redes
        InternationalLte, // Datos de consumo internacional en la red lte
        PromoBonusLte, // Bono de promoción de consumo en la red lte
        PromoBonus, // Bono de promoción de consumo internacional en todas las redes
        National, // Datos de consumo nacional en todas las redes
        DailyBag, // Bolsa diaria
        MessagingBag; // Bolsa de mensajería

        companion object {

            /**
             * Obtiene todos los [DataType] que se consumen en la red lte.
             */
            fun getDataTypeOfLte(): Array<DataType> {

                //Obtengo los DataType menos National y MessagingBag
                return values()
                    .filter { it != National && it != MessagingBag }
                    .toTypedArray()
            }

            /**
             * Obtiene todos los [DataType] que se consumen en las redes restantes.
             */
            fun getDataTypeOfInternationalNetworks(): Array<DataType> {

                //Obtengo los DataType PromoBonus e International que son los que se consumen
                // en las redes que no son lte
                return values()
                    .filter { it == PromoBonus && it == International }
                    .toTypedArray()
            }
        }
    }
}