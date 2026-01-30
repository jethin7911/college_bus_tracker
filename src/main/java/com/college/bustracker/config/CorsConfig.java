package com.college.bustracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. Allow credentials (Required if you use Cookies or Auth headers)
        config.setAllowCredentials(true);

        // 2. Specify allowed origins
        // Use addAllowedOriginPattern("*") for development to allow all while credentials are true
        config.addAllowedOriginPattern("*");

        // 3. Allow all headers
        config.addAllowedHeader("*");

        // 4. Allow specific or all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}