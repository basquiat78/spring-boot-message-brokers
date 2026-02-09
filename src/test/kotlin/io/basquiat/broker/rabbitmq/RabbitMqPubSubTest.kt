package io.basquiat.broker.rabbitmq

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.common.code.BrokerType
import io.basquiat.global.broker.common.handler.AlarmToBot
import io.basquiat.global.broker.common.handler.AlarmToLog
import io.basquiat.global.broker.common.handler.AlarmToLogHandler
import io.basquiat.global.broker.common.router.MessageRouter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class RabbitMqPubSubTest
    @Autowired
    constructor(
        private val connectionFactory: ConnectionFactory,
        private val messageRouter: MessageRouter,
    ) {
        @Test
        fun `RabbitMQ 연결 확인`() {
            connectionFactory.createConnection().use { conn ->
                println("Connected: ${conn.localPort}")
            }
        }

        @Test
        fun `RabbitMQ pub-sub 테스트`() {
            // Given: 전송할 데이터 준비
            val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // When
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.RABBITMQ, alarmToBotMessage)
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(5000)
        }

        @MockitoSpyBean
        lateinit var alarmToLogHandler: AlarmToLogHandler

        @Test
        fun `메시지 처리 실패 시 재시도 로직 검증`() {
            // given: 메소드 실행시 에러 발생하도록 mockito 설정
            doThrow(RuntimeException("에러 발생 - 리트라이 테스트"))
                .`when`(alarmToLogHandler)
                .handle(any())

            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // when
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(10000)

            // 구성 파일에서 재시도 3번을 세팅했기 때문에 3번은 실행이 되야 한다.
            verify(alarmToLogHandler, atLeast(3)).handle(any())
        }

        @Test
        fun `RabbitMQ 지연 발송 테스트`() {
            // given: 5초 지연 설정
            val delayMillis = 5000L
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // when
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage, delayMillis)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(100000)
            println("지연이후 로그 부분")
        }
    }