package io.basquiat.global.exceptions

class NatsTimeoutException(
    message: String? = "NATS 응답자가 응답하지 않습니다.",
) : RuntimeException(message)