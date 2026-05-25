package dev.iamrat.board.file.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {
    
    private final S3Properties s3Properties;

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
