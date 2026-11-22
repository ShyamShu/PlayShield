package com.payshield.transaction.Properties;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "payshield.transactions")
public record TransactionProperties(
        BigDecimal maxAmount,
        int maxPageSize,
        Duration maxFutureSkew,
        Duration maxAge
) {
}
