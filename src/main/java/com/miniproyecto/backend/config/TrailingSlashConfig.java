package com.miniproyecto.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class TrailingSlashConfig {

    /** Hace que /api/events/ y /api/events/1/ se atiendan igual que sin la barra final. */
    @Bean
    public UrlHandlerFilter trailingSlashFilter() {
        return UrlHandlerFilter.trailingSlashHandler("/api/**").wrapRequest().build();
    }
}
