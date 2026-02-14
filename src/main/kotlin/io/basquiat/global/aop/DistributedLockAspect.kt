package io.basquiat.global.aop

import io.basquiat.global.annotations.DistributedLock
import io.basquiat.global.utils.distributedLockError
import io.basquiat.global.utils.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.api.RedissonClient
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DistributedLockAspect(
    private val redissonClient: RedissonClient,
) {
    private val log = logger<DistributedLockAspect>()

    private val parser = SpelExpressionParser()

    @Around("@annotation(io.basquiat.global.annotations.DistributedLock)")
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature

        val distributedLock = signature.method.getAnnotation(DistributedLock::class.java)

        val dynamicKey =
            getDynamicValue(
                signature.parameterNames,
                joinPoint.args,
                distributedLock.key,
            )

        if (dynamicKey.isBlank()) distributedLockError("락 키를 생성할 수 없습니다. (Key: ${distributedLock.key})")

        val lockName = "lock:$dynamicKey"
        val redissonLock = redissonClient.getLock(lockName)

        // useWatchdog이 true이면 leaseTime을 -1로 설정하여 Redisson Watchdog 활성화
        // 이를 통해 락 릴리즈 시간을 자동으로 연장하도록 처리한다.
        val leaseTime = if (distributedLock.useWatchdog) -1L else distributedLock.leaseTime

        val startTime = System.currentTimeMillis()

        return try {
            val locked = redissonLock.tryLock(distributedLock.waitTime, leaseTime, TimeUnit.SECONDS)
            val waitDuration = System.currentTimeMillis() - startTime

            if (!locked) {
                log.warn("[DISTRIBUTED_LOCK] TIMEOUT | Key: {} | Wait: {}ms", lockName, waitDuration)
                distributedLockError("락 획득에 실패했습니다: $lockName")
            }

            log.info(
                "[DISTRIBUTED_LOCK] SUCCESS | Key: {} | Wait: {}ms | Watchdog: {}",
                lockName,
                waitDuration,
                distributedLock.useWatchdog,
            )

            // joinPoint 실행
            joinPoint.proceed()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            distributedLockError("락 대기 중 인터럽트: $e")
        } finally {
            // 락 해제 시 현재 스레드가 hold중인지 확실하게 체크한다.
            if (redissonLock.isHeldByCurrentThread) {
                try {
                    redissonLock.unlock()
                    log.info("락 해제 완료: $lockName")
                } catch (_: IllegalMonitorStateException) {
                    // 만료되었거나 어떤 알수 없는 이유로 해제할 수 없는 경우 방어적으로 예외 처리
                    log.warn("락이 이미 해제되었거나 소유하고 있지 않습니다: $lockName")
                } catch (e: Exception) {
                    log.error("락 해제 중 예상치 못한 에러 발생: $lockName", e)
                }
            }
        }
    }

    private fun getDynamicValue(
        parameterNames: Array<String>,
        args: Array<Any?>,
        key: String,
    ): String {
        val context = StandardEvaluationContext()
        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args[i])
        }

        return runCatching {
            parser.parseExpression(key).getValue(context, Any::class.java)?.toString()
        }.getOrNull() ?: distributedLockError("SpEL 파싱 실패 또는 결과가 null입니다. (Key: $key)")
    }
}