package io.basquiat.global.broker.common.code

import io.basquiat.global.broker.common.handler.AlarmToBot
import io.basquiat.global.broker.common.handler.AlarmToLog

/**
 * Broker pub/sub 채널 관리 dto 타입 정의
 * 채널이 늘어난다면 여기에서 채널과 dto을 정의해서 추가한다.
 */
enum class BrokerChannel(
    val channelName: String,
    val type: Class<*>,
) {
    ALARM_TO_BOT("basquiat.alarm.to.bot", AlarmToBot::class.java),
    ALARM_TO_LOG("basquiat.alarm.to.log", AlarmToLog::class.java),
    ;

    companion object {
        val allChannelNames: List<String>
            get() = entries.map { it.channelName }

        fun findByName(name: String) = entries.find { it.channelName == name }
    }
}