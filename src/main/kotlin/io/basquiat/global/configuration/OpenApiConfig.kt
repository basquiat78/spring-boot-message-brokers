package io.basquiat.global.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun groupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("spring-boot-message-brokers-API")
            .pathsToMatch("/**")
            .build()

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("스프링부트와 함께 Message Broker 서버")
                    .description("스프링부트와 함께 Message Brokers를 사용해 보자")
                    .version("v2"),
            )
}