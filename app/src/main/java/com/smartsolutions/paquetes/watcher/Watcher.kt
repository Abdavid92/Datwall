package com.smartsolutions.paquetes.watcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class Watcher : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    companion object {

    }
}