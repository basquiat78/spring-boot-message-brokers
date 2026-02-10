package io.basquiat.global.exceptions

class NotMatchMessageTypeException(
    message: String? = "채널에 맞지 않는 메시지 타입입니다.",
) : RuntimeException(message)