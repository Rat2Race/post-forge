package dev.iamrat.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {
//    @Value("${spring.cors.allowed-origins:${cors.allowed-origins}}")
//    private List<String> allowedOrigins;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        //local
        List<String> localOrigins = List.of(allowedOrigins.split(","));
        
//        log.info("=== CORS 설정 ===");
//        log.info("허용된 Origins: {}", allowedOrigins);
//        log.info("Origins 개수: {}", allowedOrigins.size());
  
        log.info("=== CORS 설정 ===");
        log.info("허용된 Origins: {}", localOrigins);
        log.info("Origins 개수: {}", localOrigins.size());
        
//        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedOrigins(localOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        log.info("CORS 설정 완료!");
        return source;
    }
}
