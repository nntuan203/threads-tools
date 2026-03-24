package com.example.autopost.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupLogger {

    @Value("${threads.access.token}")
    private String token;

    @Bean
    CommandLineRunner logToken() {
        return args -> {
            if (token == null || token.isEmpty()) {
                System.out.println("⚠️ THREADS_ACCESS_TOKEN not found");
            } else {
                int half = token.length() / 2;
                String partial = token.substring(0, half);
                System.out.println("🔑 THREADS_ACCESS_TOKEN (half): " + partial + "...");
            }
        };
    }
}