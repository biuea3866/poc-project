package com.biuea.concurrency.language

import com.biuea.concurrency.Lock
import java.util.concurrent.Semaphore

class SemaphoreLockView: Lock() {
    private val semaphore = Semaphore(3)

    override fun lock(print: Boolean) {
        semaphore.acquire()
        this.viewPage.view(true)
        semaphore.release()
    }
}