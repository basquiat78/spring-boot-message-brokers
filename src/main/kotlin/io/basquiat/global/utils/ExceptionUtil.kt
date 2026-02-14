package io.basquiat.global.utils

import io.basquiat.global.exceptions.*

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

/**
 * NatTimeoutException
 * @param message
 */
fun natsTimeout(message: String? = "NATS 응답자가 응답하지 않습니다."): Nothing =
    throw message?.let { throw NatsTimeoutException(it) }
        ?: NatsTimeoutException()

/**
 * NotFoundException 던지기
 * @param message
 */
fun notFound(message: String? = "조회된 정보가 없습니다."): Nothing = throw message?.let { throw NotFoundException(it) } ?: NotFoundException()

/**
 * OutOfStockException 던지기
 * @param message
 */
fun unableToJoin(message: String? = "해적단에 합류할 수 없습니다."): Nothing =
    throw message?.let { throw UnableToJoinException(it) }
        ?: UnableToJoinException()

/**
 * NatsResponseException
 */
fun natsResponse(message: String? = "해적단에 합류할 수 없습니다."): Nothing =
    throw message?.let { throw NatsResponseException(it) }
        ?: NatsResponseException()

/**
 * DistributedLockException
 */
fun distributedLockError(message: String? = "락 획득에 실패했습니다."): Nothing =
    throw message?.let {
        throw DistributedLockException(it)
    } ?: DistributedLockException()