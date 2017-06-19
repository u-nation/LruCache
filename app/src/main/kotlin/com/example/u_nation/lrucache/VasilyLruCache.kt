package com.example.u_nation.lrucache

import java.util.*

class VasilyLruCache<K, V>(val maxCacheSize: Long = 10 * 1024 * 1024, val maxItemCount: Int = 10, val sizeOf: (key: K, value: V) -> Int) {

    private var size: Int = 0
    private val cacheMap: LinkedHashMap<K, V> = LinkedHashMap(0, 0.75f, /*accessOrder*/ true)
    private val lock = Any()

    fun size(): Int = kotlin.synchronized(lock) { size }
    fun itemCount(): Int = kotlin.synchronized(lock) { cacheMap.size }

    fun get(key: K): V? = kotlin.synchronized(lock) { cacheMap[key] }

    fun put(key: K, value: V): V? {
        var previous: V? = null
        kotlin.synchronized(lock) {
            size += safeSizeOf(key, value)
            previous = cacheMap.put(key, value)
            previous?.let { size -= safeSizeOf(key, it) }
        }
        trimToSize()
        return previous
    }

    tailrec private fun trimToSize() {
        var key: K
        var value: V
        kotlin.synchronized(lock) {
            if (size < 0 || cacheMap.isEmpty() && size != 0) {
                throw IllegalStateException("${javaClass.name}.sizeOf() is reporting inconsistent results!")
            }
            if (cacheMap.size <= maxItemCount && size <= maxCacheSize) return@trimToSize

            val toEvict = cacheMap.entries.first()
            key = toEvict.key
            value = toEvict.value
            cacheMap.remove(key)
            size -= safeSizeOf(key, value)
        }
        trimToSize()
    }

    fun remove(key: K): V? {
        kotlin.synchronized(lock) {
            val previous: V? = cacheMap.remove(key)
            previous?.let { size -= safeSizeOf(key, it) }
            return previous
        }
    }

    private fun safeSizeOf(key: K, value: V): Int {
        val result = sizeOf(key, value)
        if (result < 0) throw IllegalStateException("sizeOf is NOT allowed to return negative size: key=$key,value=$value,size=$result")
        return result
    }
}