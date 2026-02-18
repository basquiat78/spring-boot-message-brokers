package io.basquiat.global.annotations

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedRedLock(
    val key: String,
    val waitTime: Long = 10L,
    val leaseTime: Long = 5L,
    // 락을 걸고 릴리즈하는 시간의 단위를 명시적으로 넘긴다.
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
)