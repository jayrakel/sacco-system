package com.sacco.sacco_system;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@ComponentScan(basePackages = {
    "com.sacco.sacco_system",
    "com.sacco.sacco_system.modules.*"
})
public class SaccoSystemApplication {

	public static void main(String[] args) {
		// ✅ THE MISSING LINK: Load .env file manually into System Properties
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});
			System.out.println("✅ Environment variables loaded successfully.");
		} catch (Exception e) {
			System.out.println("⚠️ No .env file found or error loading it. Falling back to system environment.");
		}

		SpringApplication.run(SaccoSystemApplication.class, args);
	}
}