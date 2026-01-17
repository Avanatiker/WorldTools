package org.waste.of.time.storage.cache

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LazyUpdatingDelegate<T>(private val timeout: Long, private val block: () -> T) : ReadOnlyProperty<Any?, T> {
    private var value: T = block()
    private var lastUpdateTime: Long = System.currentTimeMillis()

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= timeout) {
            value = block()
            lastUpdateTime = currentTime
        }
        return value
    }
}