package io.basquiat.lock

import io.basquiat.api.order.model.OrderCreateDto
import io.basquiat.api.order.usecase.PlaceOrderUsecase
import io.basquiat.domain.product.service.ProductService
import io.basquiat.global.utils.notFound
import io.basquiat.global.utils.unableToJoin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class LockTest
    @Autowired
    constructor(
        private val placeOrderUsecase: PlaceOrderUsecase,
        private val productService: ProductService,
    ) {
        @Test
        fun `Lock 테스트하기`() {
            // given
            val productId = 7L
            val quantity = 1
            val request = OrderCreateDto(productId = productId, quantity = quantity)
            // when
            val requestCount = 100
            val executorService = Executors.newFixedThreadPool(15)
            val latch = CountDownLatch(requestCount)

            val successCount =
                AtomicInteger(0)
            val failCount =
                AtomicInteger(0)

            // When
            for (i in 1..requestCount) {
                executorService.submit {
                    try {
                        val response = placeOrderUsecase.execute(request)
                        if (response.orderId == null) unableToJoin(response.error)

                        println("해적단 합류 성공 아이디: ${response.orderId}")
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        println("해적단 합류 실패 원인: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()

            val product = productService.findByIdOrNull(productId) ?: notFound("해당 보물을 찾을 수 없습니다. 해적단 합류 아이디: $productId")
            assertThat(product.quantity).isEqualTo(0)
            // 성공 횟수 20 실패 횟수 80이 나와야 한다.
            println("성공 횟수: ${successCount.get()}, 실패 횟수: ${failCount.get()}")
        }
    }