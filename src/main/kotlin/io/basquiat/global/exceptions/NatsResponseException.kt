package io.basquiat.global.exceptions

class NatsResponseException(
    message: String? = "알수 없는 장애가 발생했습니다.",
) : RuntimeException(message)