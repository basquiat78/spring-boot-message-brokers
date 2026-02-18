package io.basquiat.global.aop

import io.basquiat.global.annotations.DistributedRedLock
import io.basquiat.global.utils.distributedLockError
import io.basquiat.global.utils.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.redisson.RedissonMultiLock
import org.redisson.api.RedissonClient
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DistributedRedLockAspect(
    private val redissonClients: List<RedissonClient>,
) {
    private val log = logger<DistributedRedLockAspect>()
    private val parser = SpelExpressionParser()

    @Around("@annotation(io.basquiat.global.annotations.DistributedRedLock)")
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(DistributedRedLock::class.java)

        val dynamicKey = getDynamicValue(signature.parameterNames, joinPoint.args, annotation.key)
        if (dynamicKey.isBlank()) distributedLockError("Redlock 키를 생성할 수 없습니다. (Key: ${annotation.key})")

        val lockName = "redlock:$dynamicKey"

        // 기존과 다른 점은 3개의 독립된 Redis로부터 락을 가져온다.
        val locks = redissonClients.map { it.getLock(lockName) }

        // 이제는 RedissonMultiLock을 통해 Redlock을 사용해야 한다.
        // 2개 이상의 노드에서 성공해야 락을 획득할 수 있다. RedLock 알고리즘
        val multiLock = RedissonMultiLock(*locks.toTypedArray())

        val startTime = System.currentTimeMillis()
        log.info("[REDLOCK_ATTEMPT] Target: {} | Nodes: {}", lockName, redissonClients.size)

        return try {
            val locked =
                multiLock.tryLock(
                    annotation.waitTime,
                    annotation.leaseTime,
                    annotation.timeUnit,
                )

            val waitDuration = System.currentTimeMillis() - startTime

            if (!locked) {
                log.warn("[REDLOCK_TIMEOUT] Failed to acquire lock for {} | Wait: {}ms", lockName, waitDuration)
                distributedLockError("Redlock 획득 실패 (과반수 노드 응답 없음 또는 시간 초과): $lockName")
            }

            log.info("[REDLOCK_SUCCESS] Acquired lock for {} | Wait: {}ms", lockName, waitDuration)

            joinPoint.proceed()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            distributedLockError("Redlock 대기 중 인터럽트 발생: ${e.message}")
        } catch (e: Exception) {
            log.error("[REDLOCK_ERROR] 로직 수행 중 예외 발생: {}", e.message)
            throw e
        } finally {
            try {
                if (multiLock.isHeldByCurrentThread) {
                    multiLock.unlock()
                    log.info("[REDLOCK_RELEASE] Lock released for {}", lockName)
                }
            } catch (e: Exception) {
                log.error("[REDLOCK_RELEASE_ERROR] Lock release failed for {}: {}", lockName, e.message)
            }
        }
    }

    /**
     * SpEL 파싱 로직
     */
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
        }.getOrNull() ?: distributedLockError("SpEL 파싱 실패: $key")
    }
}