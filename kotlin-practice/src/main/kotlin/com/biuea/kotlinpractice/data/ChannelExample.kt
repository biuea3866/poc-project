package com.biuea.kotlinpractice.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun CoroutineScope.produceNumbers() = produce<Int> {
    var x = 1
    while (true) {
        send(x++)
        delay(100)
    }
}

fun CoroutineScope.square(numbers: ReceiveChannel<Int>) = produce<Int> {
    for (x in numbers) {
        send(x * x)
    }
}

fun CoroutineScope.launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
    for (msg in channel) {
        println("Processor #$id received $msg")
    }
}

fun CoroutineScope.produce(channel: Channel<Int>) {
    listOf(1, 2, 3, 4, 5).forEach { launch { channel.send(it) } }
}

fun CoroutineScope.consume(channel: Channel<Int>) {
    async(Dispatchers.Default) {
        for (el in channel) {
            println("value: $el")
        }
    }
}

fun main() {
    runBlocking {
        val channel = Channel<Int>(capacity = 5, onBufferOverflow = BufferOverflow.SUSPEND)

        consume(channel)

        produce(channel)

//        channel.close()

//        repeat(5) { launchProcessor(it, numbers) }
//
//        val numbers = produceNumbers()  // 1, 2, 3, 4, 5, ...
//        val squares = square(numbers)   // 1, 4, 9, 16, 25, ...

//        repeat(5) {
//            println(squares.receive())
//        }

//        repeat(5) { launchProcessor(it, numbers) }

//        delay(1000)

//        coroutineContext.cancelChildren()  // 모든 자식 취소
    }
}