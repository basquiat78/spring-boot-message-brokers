package io.basquiat.global.broker.common.handler

data class AlarmToBot(
    val message: String,
)

data class AlarmToLog(
    val message: String,
    val extra: String? = null,
)