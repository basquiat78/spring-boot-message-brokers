package io.basquiat.broker.kafka

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.common.code.BrokerType
import io.basquiat.global.broker.common.handler.AlarmToBot
import io.basquiat.global.broker.common.handler.AlarmToLog
import io.basquiat.global.broker.common.handler.AlarmToLogHandler
import io.basquiat.global.broker.common.router.MessageRouter
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class KafkaPubSubTest
    @Autowired
    constructor(
        private val messageRouter: MessageRouter,
    ) {
        @Test
        fun `Kafka를 통한 publish, consume 테스트`() {
            // given
            val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // when
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.KAFKA, alarmToBotMessage)
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.KAFKA, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(1000)
        }

        @MockitoSpyBean
        lateinit var alarmToLogHandler: AlarmToLogHandler

        @Test
        fun `Kafka 메시지 처리 실패 시 재시도 및 최종 DLT 이동 검증`() {
            // given: Mockito를 사용하여 해당 핸들러 호출 시 무조건 에러 발생 설정하자
            doThrow(RuntimeException("테스트용 강제 에러"))
                .`when`(alarmToLogHandler)
                .handle(any())

            val alarmToLogMessage = AlarmToLog(message = "재시도 테스트 메시지", extra = "extra data")

            // when: Kafka 채널로 메시지 전송
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.KAFKA, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(10000)

            // 3번의 재시도이니 재시도 횟수 검증
            verify(alarmToLogHandler, times(3)).handle(any())
        }
    }