package no.nav.helse.flex.infrastructure.resilience

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.vavr.CheckedFunction1
import io.vavr.control.Try
import java.io.IOException
import java.net.http.HttpResponse
import java.util.concurrent.TimeoutException

class Resilience<T, R>(function: CheckedFunction1<T, R>) {
    private val functionWithResilience: CheckedFunction1<T, R>
    private val circuitBreaker: CircuitBreaker
    private val retry: Retry

    init {
        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .recordExceptions(IOException::class.java, TimeoutException::class.java)
            .build()
        val retryConfig = RetryConfig.custom<HttpResponse<String>>()
            .maxAttempts(3)
            .retryOnResult { response: HttpResponse<String> -> response.statusCode() > 499 }
            .build()
        circuitBreaker = CircuitBreaker.of("CircutBreakerName", circuitBreakerConfig)
        retry = Retry.of("RetryName", retryConfig)
        functionWithResilience = createResilience(function)
    }

    private fun createResilience(function: CheckedFunction1<T, R>): CheckedFunction1<T, R> {
        return Decorators.ofCheckedFunction(function)
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .decorate()
    }

    fun execute(functionParameter: T): R {
        val trier = Try.of { functionWithResilience.apply(functionParameter) }
        return trier.get()
    }
}
