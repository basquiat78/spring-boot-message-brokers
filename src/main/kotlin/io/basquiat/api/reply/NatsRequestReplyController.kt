package io.basquiat.api.reply

import io.basquiat.api.reply.usecase.HelloRequestUsecase
import io.basquiat.global.utils.logger
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/nats")
@Tag(name = "NATS Request-Reply Controller")
class NatsRequestReplyController(
    private val helloRequestUsecase: HelloRequestUsecase,
) {
    private val log = logger<NatsRequestReplyController>()

    @GetMapping("/request/hello")
    fun hello(): String = helloRequestUsecase.execute()

//    /**
//     * nats core를 이용한 request-reply
//     */
//    @GetMapping("/request/hello")
//    fun hello(): String {
//        val subject = "nats.bow.hello"
//        val payload = "hello".toByteArray()
//        val replyFuture = natsConnection.request(subject, payload)
//        return try {
//            val reply = replyFuture.get(3, TimeUnit.SECONDS)
//            String(reply.data)
//        } catch (e: Exception) {
//            log.error("error message: $e")
//            "에러 발생: ${e.message}"
//        }
//    }
}