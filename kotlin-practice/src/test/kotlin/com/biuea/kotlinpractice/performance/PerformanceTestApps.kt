package com.biuea.kotlinpractice.performance

import kotlinx.coroutines.delay
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Duration

const val SIMULATED_DELAY_MS = 100L

// ============================================================
// Controllers
// ============================================================

@RestController
class BlockingController {
    @GetMapping("/performance/test")
    fun test(): String {
        Thread.sleep(SIMULATED_DELAY_MS)
        return "OK"
    }
}

@RestController
class ReactiveController {
    @GetMapping("/performance/test")
    fun test(): Mono<String> {
        return Mono.delay(Duration.ofMillis(SIMULATED_DELAY_MS)).thenReturn("OK")
    }
}

@RestController
class CoroutineController {
    @GetMapping("/performance/test")
    suspend fun test(): String {
        delay(SIMULATED_DELAY_MS)
        return "OK"
    }
}

// ============================================================
// Application Configurations (each creates an isolated context)
// ============================================================

/** Scenario 1, 5: Tomcat MVC + Blocking */
@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@Import(BlockingController::class)
class TomcatMvcBlockingApp

/** Scenario 2, 6: Netty WebFlux + Reactive */
@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@Import(ReactiveController::class)
class NettyWebFluxReactiveApp

/** Scenario 4, 8: Netty WebFlux + Coroutine */
@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@Import(CoroutineController::class)
class NettyWebFluxCoroutineApp

/** Scenario 7: Tomcat MVC + Coroutine */
@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@Import(CoroutineController::class)
class TomcatMvcCoroutineApp
