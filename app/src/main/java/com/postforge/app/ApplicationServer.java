package com.postforge.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.postforge")
@EntityScan(basePackages = "com.postforge")
@EnableJpaRepositories(basePackages = "com.postforge")
@EnableJpaAuditing
@EnableScheduling
public class ApplicationServer {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationServer.class, args);
    }
}
