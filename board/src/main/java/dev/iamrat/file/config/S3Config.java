package dev.iamrat.file.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("s3")
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {
    
    private final S3Properties s3Properties;

    @Bean
    public S3Client s3Client(Region region, DefaultCredentialsProvider credentialsProvider) {
        return S3Client.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean
    public S3Presigner s3Presigner(Region region, DefaultCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .build();
    }

    @Bean
    public Region awsRegion() {
        return Region.of(s3Properties.region());
    }

    @Bean
    public DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}
