package io.basquiat

import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.*

@SpringBootApplication
class SpringWithBrokersApplication

@PostConstruct
fun initTimezone() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
}

fun main(args: Array<String>) {
    runApplication<SpringWithBrokersApplication>(*args)
}