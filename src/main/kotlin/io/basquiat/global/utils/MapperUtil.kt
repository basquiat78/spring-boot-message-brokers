package io.basquiat.global.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * kotlin jackson object mapper
 */
val mapper: ObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        enable(SerializationFeature.INDENT_OUTPUT)
    }

/**
 * 객체를 받아서 json 스트링으로 반환한다.
 *
 * @param any
 * @return String
 */
fun <T> objectToString(any: T): String = mapper.writeValueAsString(any)

fun <T> jsonToObject(
    json: String,
    valueType: Class<T>,
): T = mapper.readValue(json, valueType)

fun <T> byteToObject(
    data: ByteArray,
    valueType: Class<T>,
): T = mapper.readValue(data, valueType)

fun <T> convertToTarget(
    source: Any,
    target: Class<T>,
): T = mapper.convertValue(source, target)

fun <T> objectToByte(any: T): ByteArray = mapper.writeValueAsBytes(any)

fun readObjectTree(content: String): JsonNode = mapper.readTree(content)