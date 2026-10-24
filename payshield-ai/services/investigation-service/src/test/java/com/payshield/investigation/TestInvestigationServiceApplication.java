package com.payshield.investigation;

import org.springframework.boot.SpringApplication;

public class TestInvestigationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(InvestigationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
