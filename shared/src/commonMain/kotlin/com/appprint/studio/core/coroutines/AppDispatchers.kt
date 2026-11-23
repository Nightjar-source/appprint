package com.appprint.studio.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Platform-appropriate dispatcher for file/image work.
 *
 * `Dispatchers.IO` is a JVM/Android-only member in common coroutines, while this
 * module also compiles to Kotlin/Wasm. The actuals keep the elastic thread pool
 * on JVM/Android and fall back to the default dispatcher where IO has no meaning.
 */
expect object AppDispatchers {
    val io: CoroutineDispatcher
}
