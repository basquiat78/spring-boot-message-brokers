package io.basquiat.api.init

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/init")
@Tag(name = "init controller")
class InitController {
    @GetMapping("/hello")
    fun init(): String = "hello!"
}