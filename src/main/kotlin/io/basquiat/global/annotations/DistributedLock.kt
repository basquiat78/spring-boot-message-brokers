package io.basquiat.global.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTime: Long = 5L,
    val leaseTime: Long = 3L,
    val useWatchdog: Boolean = true,
)