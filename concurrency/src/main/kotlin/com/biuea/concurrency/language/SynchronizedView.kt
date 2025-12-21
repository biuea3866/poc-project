package com.biuea.concurrency.language

import com.biuea.concurrency.Lock
import com.biuea.concurrency.ViewPage
import kotlin.jvm.Synchronized

class SynchronizedView: Lock() {
    @Synchronized
    override fun lock(print: Boolean) {
        this.viewPage.view(print)
    }
}