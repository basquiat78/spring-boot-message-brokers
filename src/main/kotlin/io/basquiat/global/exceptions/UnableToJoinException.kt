package io.basquiat.global.exceptions

class UnableToJoinException(
    message: String? = "해적단에 합류할 수 없습니다.",
) : RuntimeException(message)