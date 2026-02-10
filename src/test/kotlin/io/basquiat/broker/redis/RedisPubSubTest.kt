package io.basquiat.broker.redis

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.common.code.BrokerType
import io.basquiat.global.broker.common.handler.AlarmToBot
import io.basquiat.global.broker.common.handler.AlarmToLog
import io.basquiat.global.broker.common.router.MessageRouter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class RedisPubSubTest
    @Autowired
    constructor(
        private val messageRouter: MessageRouter,
    ) {
        @Test
        fun `REDIS를 통한 publish, consume 테스트`() {
            // when
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.REDIS, AlarmToBot(message = "봇으로 알람 보내기 from Redis"))
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.REDIS, AlarmToLog(message = "로그 봇으로 알람 보내기 from Redis"))
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.RABBITMQ, AlarmToBot(message = "봇으로 알람 보내기 from RabbitMQ"))
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, AlarmToLog(message = "로그 봇으로 알람 보내기 from RabbitMQ"))

            // then
            Thread.sleep(5000)
        }
    }