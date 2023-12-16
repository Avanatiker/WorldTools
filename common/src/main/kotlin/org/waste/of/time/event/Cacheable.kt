package org.waste.of.time.event

interface Cacheable {
    fun cache()

    fun flush()
}