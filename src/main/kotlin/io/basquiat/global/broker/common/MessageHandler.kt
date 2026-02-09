package io.basquiat.global.broker.common

import io.basquiat.global.broker.common.code.BrokerChannel

interface MessageHandler<T> {
    val channel: BrokerChannel

    fun handle(message: T)
}