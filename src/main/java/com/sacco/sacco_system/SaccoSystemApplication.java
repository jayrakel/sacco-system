package com.sacco.sacco_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // ✅ Import this

@SpringBootApplication
@EnableScheduling // ✅ Add this annotation
public class SaccoSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SaccoSystemApplication.class, args);
	}

}