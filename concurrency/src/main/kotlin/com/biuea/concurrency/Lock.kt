package com.biuea.concurrency

abstract class Lock(
    protected val viewPage: ViewPage = ViewPage()
) {
    val count get() = this.viewPage.count

    abstract fun lock(print: Boolean)
}