package dev.iamrat.board.view.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ViewCountProperties.class)
public class ViewCountConfig {
}
