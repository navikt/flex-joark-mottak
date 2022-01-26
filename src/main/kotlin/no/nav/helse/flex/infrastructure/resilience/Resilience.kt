package no.nav.helse.flex.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.vavr.CheckedFunction1;
import io.vavr.control.Try;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

public class Resilience<T, R> {
    private final CheckedFunction1<T, R> functionWithResilience;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public Resilience(final CheckedFunction1<T,R> function) {

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .recordExceptions(IOException.class, TimeoutException.class)
                .build();
        RetryConfig retryConfig = RetryConfig.<HttpResponse<String>>custom()
                .maxAttempts(3)
                .retryOnResult(response -> response.statusCode() > 499)
                .build();
        this.circuitBreaker = CircuitBreaker.of("CircutBreakerName", circuitBreakerConfig);
        this.retry = Retry.of("RetryName", retryConfig);
        this.functionWithResilience = createResilience(function);
    }

    private CheckedFunction1<T, R> createResilience(final CheckedFunction1<T,R> function) {
        return Decorators.ofCheckedFunction(function)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .decorate();
    }

    public R execute(final T functionParameter) {
        final Try<R> trier = Try.of(() -> this.functionWithResilience.apply(functionParameter));
        return trier.get();
    }
}
