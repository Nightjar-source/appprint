package com.appprint.studio.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual object AppDispatchers {
    // Kotlin/Wasm runs on the browser event loop; Default is the only scheduler.
    actual val io: CoroutineDispatcher = Dispatchers.Default
}
