package com.everis.savingaccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class SavingAccountApplication {

	public static void main(String[] args) {
		SpringApplication.run(SavingAccountApplication.class, args);
		System.out.println("Servicio de cuentas de ahorro activado.");
	}

}
