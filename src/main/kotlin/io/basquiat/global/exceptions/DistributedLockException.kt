package io.basquiat.global.exceptions

class DistributedLockException(
    message: String? = "락 획득에 실패했습니다.",
) : RuntimeException(message)