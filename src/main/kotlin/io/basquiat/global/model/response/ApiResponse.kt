package io.basquiat.global.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.LocalDateTime
import java.time.LocalDateTime.now

/**
 * Rest API response 정보를 담은 객체
 */
@JsonPropertyOrder("path", "status", "result", "pagination", "timestamp")
data class ApiResponse<T>(
    val path: String,
    val status: Int,
    val result: T,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val pagination: Pagination?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val timestamp: LocalDateTime,
) {
    companion object {
        /**
         * ApiResponse를 생성하는 정적 메소드
         * @param status
         * @param data
         * @param path
         * @return ResponseResult<T>
         */
        fun <T> create(
            path: String,
            status: Int,
            data: T,
            pagination: Pagination? = null,
        ) = ApiResponse(
            path = path,
            status = status,
            result = data,
            pagination = pagination,
            timestamp = now(),
        )
    }
}