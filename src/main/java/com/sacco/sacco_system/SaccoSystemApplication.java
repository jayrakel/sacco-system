package com.sacco.sacco_system;

import com.sacco.sacco_system.annotation.Loggable;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync      // ✅ Enables background emails
@EnableScheduling // ✅ Enables automated tasks (Interest, Penalties)
@EnableJpaRepositories(basePackages = "com.sacco.sacco_system.modules")
@Loggable         // ✅ Enables Audit Logging for the main app
public class SaccoSystemApplication {

    public static void main(String[] args) {
        // ✅ Load .env file manually into System Properties
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