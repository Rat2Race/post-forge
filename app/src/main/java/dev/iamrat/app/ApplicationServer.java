package dev.iamrat.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "dev.iamrat")
@EntityScan(basePackages = "dev.iamrat")
@EnableJpaRepositories(basePackages = "dev.iamrat")
public class ApplicationServer {
    public static void main(String[] args) {
        SpringApplication.run(ApplicationServer.class, args);
    }
}
