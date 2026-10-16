package com.smartsolutions.paquetes.ui

import androidx.lifecycle.AndroidViewModel
import com.smartsolutions.paquetes.DatwallApplication

/**
 * Vuelve a ejecutar el arranque; las pantallas que siguen el destino ([followStartDestination])
 * abren la que corresponda.
 * */
fun AndroidViewModel.next() {
    getApplication<DatwallApplication>().main()
}
