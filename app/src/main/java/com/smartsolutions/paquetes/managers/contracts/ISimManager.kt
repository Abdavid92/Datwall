package com.smartsolutions.paquetes.managers.contracts

import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow

interface ISimManager {

    /**
     * Verifica que existan dos subscriptionInfo activos
     * @return  - true si hay mas de una Sim instalada
     */
    fun isSeveralSimsInstalled(): Boolean

    /**
     * Busca las lineas que están activas, las crea si no existen en el repositorio o sincroniza sus
     * valores con el sistema y actualiza el repositorio
     *
     * @param relations - Indica si se devuelven las sim con sus relaciones foráneas
     *
     * @return - La lista de todas las Sim instaladas con los valores actuales
     **/
    suspend fun getInstalledSims(relations: Boolean = false): List<Sim>

    /**
     * Obtiene la línea que esta marcada como predeterminada para el tipo dado
     * @param type - El tipo de Sim predeterminada que se quiere obtener [SimDelegate.SimType.DATA]
     * o [SimDelegate.SimType.VOICE]
     * @param relations - Indica si se devuelven las sim con las relaciones foráneas
     * @return - En los SDK 24 o superiores se retorna la Sim predeterminada por el Sistema para el tipo dado,
     * en SDK 23 o inferior se retorna la Sim marcada como predeterminada en el reporsitorio
     * @throws IllegalStateException - Si en SDK 23 o inferior no existe una Sim marcada como
     * predeterminada en el repositorio o hay mas de una Sim marcada como predeterminada
     */
    suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean = false): Sim

    /**
     * Establece en el repositorio la Sim dada como predeterminada para el tipo dado. Esta función
     * solamente es necesaria en SDK 23 o inferior
     * @param type - El tipo que se establecerá como predeterminado [SimDelegate.SimType.DATA] o
     * [SimDelegate.SimType.VOICE]
     * @param sim - La Sim que se establece el valor predeterminado
     */
    suspend fun setDefaultSim(type: SimDelegate.SimType, sim: Sim)

    /**
     * Obtiene la Sim instalada que se encuentre en el slot dado
     * @param slotIndex - El índice de la ranura donde está la Sim
     * @param relations - Indica si se devuelve la Sim con las relaciones foráneas
     * @return - La Sim que se encuentra instalada en la ranura o null si no hay ninguna
     */
    suspend fun getSimBySlotIndex(slotIndex: Int, relations: Boolean = false): Sim?

    /**
     * Conección con el repositorio de Sim. Se dispara cada vez que se produce un cambio en el mismo
     * @param relations - Indica si se devueve la Sim con las relaciones foráneas
     * @return - Un Flow conectado al repositorio que contiene solamente las Sim instaldas actualizadas
     */
    fun flowInstalledSims(relations: Boolean = false): Flow<List<Sim>>

    /**
     * Registra un oyente para detectar cualquier cambio que se produzca con las sims.
     * */
    fun registerSubscriptionChangedListener()

    /**
     * Quita el registro del oyente de los cambios de sims.
     * */
    fun unregisterSubscriptionChangedListener()
}