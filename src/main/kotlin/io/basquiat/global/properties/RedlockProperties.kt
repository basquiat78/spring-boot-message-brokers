package io.basquiat.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "redlock")
data class RedlockProperties
    @ConstructorBinding
    constructor(
        val nodes: List<RedlockNode>,
        val database: Int = 0,
        val timeout: Int = 3000,
    )

data class RedlockNode(
    val address: String = "",
    val password: String? = null,
)