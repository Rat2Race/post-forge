package dev.iamrat.source.product.infrastructure.naver;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(NaverShoppingProperties.class)
public class NaverShoppingSourceConfig {
}
