package io.basquiat.global.broker.common.handler

import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.utils.logger
import org.springframework.stereotype.Component

@Component
class AlarmToBotHandler : MessageHandler<AlarmToBot> {
    private val log = logger<AlarmToBotHandler>()

    override val channel = BrokerChannel.ALARM_TO_BOT

    override fun handle(message: AlarmToBot) {
        log.info("봇으로 보내는 알람 메세지 정보: $message")
    }
}

@Component
class AlarmToLogHandler : MessageHandler<AlarmToLog> {
    private val log = logger<AlarmToLogHandler>()

    override val channel = BrokerChannel.ALARM_TO_LOG

    override fun handle(message: AlarmToLog) {
        log.info("로그로 보내는 알람 메세지 정보: $message")
    }
}