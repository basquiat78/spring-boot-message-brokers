package io.basquiat.cache

import io.basquiat.api.product.usecase.FetchProductUsecase
import io.basquiat.global.utils.objectToString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class CacheTest
    @Autowired
    constructor(
        private val fetchProductUsecase: FetchProductUsecase,
    ) {
        @Test
        fun `Cache 테스트하기`() {
            // given
            val productId = 7L

            // when
            val requestCount = 50
            val executorService = Executors.newFixedThreadPool(15)
            val latch = CountDownLatch(requestCount)

            // When
            for (i in 1..requestCount) {
                executorService.submit {
                    try {
                        val response = fetchProductUsecase.execute(productId)
                        println(objectToString(response))
                    } catch (e: Exception) {
                        println("해적단 합류 실패 원인: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
        }
    }