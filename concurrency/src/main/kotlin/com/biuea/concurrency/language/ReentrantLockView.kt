package com.biuea.concurrency.language

import com.biuea.concurrency.Lock
import com.biuea.concurrency.ViewPage
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ReentrantLockView: Lock() {
    val reentrantLock = ReentrantLock()

    override fun lock(print: Boolean) {
        this.reentrantLock.withLock { this.viewPage.view(print) }
    }
}