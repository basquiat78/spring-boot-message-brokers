package io.basquiat.global.extensions

import io.basquiat.global.utils.notFound
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull

/**
 * null을 반환하지 않고 OptionalEmptyException에러를 날리자.
 */
fun <T : Any, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(
    id: ID,
    message: String? = null,
): T = this.findByIdOrNull(id) ?: notFound(message)