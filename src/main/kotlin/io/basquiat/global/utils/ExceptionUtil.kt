package io.basquiat.global.utils

import io.basquiat.global.exceptions.NotMatchMessageTypeException
import io.basquiat.global.exceptions.NotSupportBrokersException

/**
 * NotSupportBrokerException 던지기
 * @param message
 */
fun notSupportBrokers(message: String? = "지원하지 않는 브로커 타입입니다"): Nothing =
    throw message?.let { throw NotSupportBrokersException(it) }
        ?: NotSupportBrokersException()

/**
 * NotMatchMessageTypeException 던지기
 * @param message
 */
fun notMatchMessageType(message: String? = "채널에 맞지 않는 메시지 타입입니다."): Nothing =
    throw message?.let { throw NotMatchMessageTypeException(it) }
        ?: NotMatchMessageTypeException()