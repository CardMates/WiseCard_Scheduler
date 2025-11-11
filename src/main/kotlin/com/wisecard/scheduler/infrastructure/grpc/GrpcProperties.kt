package com.wisecard.scheduler.infrastructure.grpc
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "grpc")
class GrpcProperties {
    lateinit var host: String
    var port: Int = 0
}