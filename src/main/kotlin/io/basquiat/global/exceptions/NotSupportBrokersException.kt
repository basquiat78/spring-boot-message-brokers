package io.basquiat.global.exceptions

class NotSupportBrokersException(
    message: String? = "지원하지 않는 브로커 타입입니다",
) : RuntimeException(message)