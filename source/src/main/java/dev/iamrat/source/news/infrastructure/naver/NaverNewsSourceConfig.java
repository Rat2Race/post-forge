package dev.iamrat.source.news.infrastructure.naver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NaverNewsProperties.class)
public class NaverNewsSourceConfig {
}
