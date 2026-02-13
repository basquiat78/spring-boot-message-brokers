package io.basquiat.global.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDateTime

@JsonPropertyOrder("path", "status", "message", "timestamp")
data class ApiError(
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val path: String? = null,
    val status: Int,
    val message: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val timestamp: LocalDateTime,
)