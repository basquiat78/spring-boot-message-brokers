package io.basquiat.global.model.vo

/**
 *
 * 이 부분만 사용될 수 있기에 abstract가 아닌 open class로 생성한다.
 */
open class PaginationVo(
    val size: Long?,
    val lastId: Long? = null,
) {
    /**
     * 쿼리용
     */
    fun limit(): Long = (size?.takeIf { it > 0 } ?: 10) + 1

    /**
     * 넘어온 size 정보용
     */
    fun size(): Long = size?.takeIf { it > 0 } ?: 10
}