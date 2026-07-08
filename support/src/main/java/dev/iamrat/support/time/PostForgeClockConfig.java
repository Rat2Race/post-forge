package dev.iamrat.support.time;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PostForgeTimeProperties.class)
public class PostForgeClockConfig {

    @Bean
    public Clock clock(PostForgeTimeProperties properties) {
        return Clock.system(properties.getZone());
    }
}
