package org.waste.of.time.storage

interface Cacheable {
    fun cache()

    fun flush()
}