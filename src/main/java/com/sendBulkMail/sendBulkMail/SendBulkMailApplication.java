package com.sendBulkMail.sendBulkMail;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SendBulkMailApplication {

	public static void main(String[] args) {
		// Load .env file
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();
		
		System.out.println("DEBUG: Loading .env entries...");
		// Map .env variables to System properties for Spring to pick up
		dotenv.entries().forEach(entry -> {
			System.out.println("DEBUG: Setting " + entry.getKey());
			System.setProperty(entry.getKey(), entry.getValue());
		});
		
		System.out.println("DEBUG: GOOGLE_CLIENT_ID property: " + System.getProperty("GOOGLE_CLIENT_ID"));
		
		SpringApplication.run(SendBulkMailApplication.class, args);
	}

}
