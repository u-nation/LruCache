package com.example.u_nation.lrucache

import android.graphics.Bitmap
import com.sys1yagi.kmockito.invoked
import com.sys1yagi.kmockito.mock
import com.taroid.knit.should
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors


class VasilyLruCacheTest {
    private lateinit var cacheMap: VasilyLruCache<Int, Bitmap>
    private lateinit var oneKbBitmap: Bitmap
    private lateinit var oneMbBitmap: Bitmap
    private lateinit var tenMbBitmap: Bitmap
    private lateinit var fiftyMbBitmap: Bitmap


    @Before
    fun setUp() {
        cacheMap = VasilyLruCache<Int, Bitmap>(sizeOf = { _, value -> value.byteCount })
        oneKbBitmap = mock<Bitmap>().apply { byteCount.invoked.thenReturn(1024) }
        oneMbBitmap = mock<Bitmap>().apply { byteCount.invoked.thenReturn(1048576) }
        tenMbBitmap = mock<Bitmap>().apply { byteCount.invoked.thenReturn(10485760) }
        fiftyMbBitmap = mock<Bitmap>().apply { byteCount.invoked.thenReturn(52428800) }
    }

    @Test
    @Throws(Exception::class)
    fun get() {
        cacheMap.put(1, tenMbBitmap)
        cacheMap.get(1).should be tenMbBitmap
    }

    @Test
    @Throws(Exception::class)
    fun remove() {
        cacheMap.put(1, tenMbBitmap)
        cacheMap.remove(1).should be tenMbBitmap
        cacheMap.itemCount().should be 0
    }

    @Test
    @Throws(Exception::class)
    fun canNotCacheOverMaxCacheSize() {
        cacheMap.put(1, fiftyMbBitmap)
        cacheMap.itemCount().should be 0
        cacheMap.size().should be 0
    }

    @Test
    @Throws(Exception::class)
    fun maxItemCountIsTen() {
        for (i in 1..20) {
            cacheMap.put(i, oneKbBitmap)
        }
        cacheMap.itemCount().should be 10
    }

    @Test
    @Throws(Exception::class)
    fun maxCacheSizeIsTenMb() {
        for (i in 1..20) {
            cacheMap.put(i, oneMbBitmap)
        }
        cacheMap.size().should be 10485760
    }

    @Test
    @Throws(Exception::class)
    fun itemCountIsOneIfCacheTenMb() {
        for (i in 1..20) {
            cacheMap.put(i, oneMbBitmap)
        }
        cacheMap.put(21, tenMbBitmap)
        cacheMap.itemCount().should be 1
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun illegalSafeSizeOf() {
        VasilyLruCache<Int, Bitmap>(sizeOf = { _, _ -> -1 }).put(1, oneMbBitmap)
    }

    @Test
    @Throws(Exception::class)
    fun threadSafe() {
        val cacheMap = VasilyLruCache<Int, Bitmap>(maxItemCount = 3000, sizeOf = { _, value -> value.byteCount })
        val task1 = Runnable { for (i in 1..1000) cacheMap.put(i, oneKbBitmap) }
        val task2 = Runnable { for (i in 1001..2000) cacheMap.put(i, oneKbBitmap) }
        val task3 = Runnable { for (i in 2001..3000) cacheMap.put(i, oneKbBitmap) }

        val executorService = Executors.newFixedThreadPool(3)
        val future1 = executorService.submit(task1, true)
        val future2 = executorService.submit(task2, true)
        val future3 = executorService.submit(task3, true)

        if (future1.get() && future2.get() && future3.get()) {
            cacheMap.size().should be 1024 * 3000
        }
        executorService.shutdown()
    }
}