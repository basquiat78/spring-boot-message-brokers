package io.basquiat.global.listener

import io.basquiat.global.utils.logger
import io.nats.client.*

/**
 * 프로덕트 레벨에서 모니터링을 위한 에러 리스너
 */
class NatsErrorListener : ErrorListener {
    private val log = logger<NatsErrorListener>()

    /**
     * 서버 오류 발생 시 호출된다.
     */
    override fun errorOccurred(
        conn: Connection?,
        error: String?,
    ) {
        log.error("Error Occurred: $error")
    }

    /**
     * 컨슈머의 처리 속도가 느려진다면 이 부분에 로그가 찍힌다.
     */
    override fun slowConsumerDetected(
        conn: Connection?,
        consumer: Consumer?,
    ) {
        val subject =
            if (consumer is Subscription) {
                consumer.subject
            } else {
                "Unknown Subject"
            }
        log.warn(
            """
            Slow Consumer Detected:
            - Subject: $subject
            - Pending Messages: ${consumer?.pendingMessageCount} / ${consumer?.pendingMessageLimit}
            - Pending Bytes: ${consumer?.pendingByteCount} / ${consumer?.pendingByteLimit}
            - Dropped Messages: ${consumer?.droppedCount}
            """.trimIndent(),
        )
    }

    /**
     * 클라이언트 컨슈머 장애 발생시
     */
    override fun exceptionOccurred(
        conn: Connection?,
        e: Exception?,
    ) {
        log.error("NATS 클라이언트 예외 발생: ${e?.message}", e)
    }

    /**
     * jetStream을 위한 heartbeat 에러
     */
    override fun heartbeatAlarm(
        conn: Connection?,
        jetStreamSubscription: JetStreamSubscription?,
        lastStreamSeq: Long,
        lastConsumerSeq: Long,
    ) {
        log.warn("jetStream Heartbeat Alarm: check this Subject: ${jetStreamSubscription?.subject}")
    }
}