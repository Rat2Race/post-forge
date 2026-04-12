package dev.iamrat.file.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("s3")
public class S3Config {

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
    public Region awsRegion(@Value("${cloud.aws.region.static}") String region) {
        return Region.of(region);
    }

    @Bean
    public DefaultCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }
}
