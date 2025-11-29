package com.biuea.concurrency

import java.util.concurrent.atomic.AtomicInteger

class ViewPage(
    private var _count: Int = 0
) {
    val count get() = this._count

    fun view(print: Boolean) {
        this._count++

        if (print) {
            println("${Thread.currentThread().name} - current view count: ${this._count}")
        }
    }
}

class VolatileViewPage(
    @Volatile var _count: Int = 0
) {
    val count get() = this._count

    fun view() {
        this._count++
        println("${Thread.currentThread().name} - current view count: ${this._count}")
    }
}

class AtomicView(
    private var _count: AtomicInteger = AtomicInteger(0)
) {
    val count get() = this._count

    fun view(print: Boolean) {
        this._count.incrementAndGet()

        if (print) {
            println("${Thread.currentThread().name} - current view count: ${this._count}")
        }
    }
}