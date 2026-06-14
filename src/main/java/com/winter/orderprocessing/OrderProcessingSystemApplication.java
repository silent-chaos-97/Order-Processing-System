package com.winter.orderprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
public class OrderProcessingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderProcessingSystemApplication.class, args);
	}

}
