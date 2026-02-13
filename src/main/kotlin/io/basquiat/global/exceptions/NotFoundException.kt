package io.basquiat.global.exceptions

class NotFoundException(
    message: String? = "조회된 정보가 없습니다.",
) : RuntimeException(message)