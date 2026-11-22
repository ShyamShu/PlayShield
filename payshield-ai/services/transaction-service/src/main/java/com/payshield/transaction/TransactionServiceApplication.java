package com.payshield.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.payshield.transaction.Properties.TransactionProperties;

@SpringBootApplication
@EnableConfigurationProperties(
		value = {
				TransactionProperties.class
		}
)
public class TransactionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransactionServiceApplication.class, args);
	}

}
