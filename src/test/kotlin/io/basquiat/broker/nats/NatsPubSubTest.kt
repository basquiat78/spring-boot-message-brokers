package io.basquiat.broker.nats

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.common.code.BrokerType
import io.basquiat.global.broker.common.handler.AlarmToBot
import io.basquiat.global.broker.common.handler.AlarmToLog
import io.basquiat.global.broker.common.router.MessageRouter
import io.nats.client.Connection
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class NatsPubSubTest
    @Autowired
    constructor(
        private val messageRouter: MessageRouter,
        private val natsConnection: Connection,
    ) {
        @Test
        fun `NATS를 통한 fan-out, push 방식의 publish, consume 테스트`() {
            // given
            val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // when
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.NATS, alarmToBotMessage)
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.NATS, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(1000)
        }
    }