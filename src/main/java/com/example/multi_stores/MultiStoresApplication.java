package com.example.multi_stores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultiStoresApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiStoresApplication.class, args);
	}

}
