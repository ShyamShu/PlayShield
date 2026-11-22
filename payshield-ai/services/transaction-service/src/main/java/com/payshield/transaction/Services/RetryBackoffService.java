package com.payshield.transaction.Services;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

@Service
public class RetryBackoffService {

    private static final long BASE_DELAY_SECONDS = 5;
    private static final long MAX_DELAY_SECONDS = 900;

    public Instant calculateNextAttempt(int currentAttempts) {

        int exponent = Math.min(currentAttempts, 20);

        long baseDelay = BASE_DELAY_SECONDS * (1L << exponent);

        baseDelay = Math.min(baseDelay, MAX_DELAY_SECONDS);

        // Random multiplier between 0.5 and 1.5.
        double jitter = ThreadLocalRandom.current()
                .nextDouble(0.5, 1.5);

        long finalDelay = Math.min(
                Math.round(baseDelay * jitter),
                MAX_DELAY_SECONDS
        );

        return Instant.now().plus(
                Duration.ofSeconds(finalDelay)
        );
    }
}