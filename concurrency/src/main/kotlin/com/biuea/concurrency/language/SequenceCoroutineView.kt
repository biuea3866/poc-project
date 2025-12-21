package com.biuea.concurrency.language

import com.biuea.concurrency.Lock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class SequenceCoroutineView {
    val dataList = listOf(
        sequence {
            (0..10_000).chunked(100).forEach { chunk ->
                yieldAll(chunk)
            }
        },
        sequence {
            (10_000..20_000).chunked(100).forEach { chunk ->
                yieldAll(chunk)
            }
        }
    )

    suspend fun lock() {
        coroutineScope {
            dataList.forEach {
                launch(Dispatchers.IO) {
                    it.chunked(1000).forEach { value ->
                        println("${Thread.currentThread().name}: ${value.toList()}")
                    }
                }
            }
        }
    }
}