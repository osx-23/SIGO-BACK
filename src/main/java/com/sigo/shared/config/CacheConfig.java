package com.sigo.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PLAZAS = "catalogo-plazas";
    public static final String TURNOS = "catalogo-turnos";
    public static final String MOTIVOS_AUSENCIA = "catalogo-motivos-ausencia";

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager =
                new CaffeineCacheManager(
                        PLAZAS,
                        TURNOS,
                        MOTIVOS_AUSENCIA
                );

        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(
                                Duration.ofMinutes(10)
                        )
        );

        return manager;
    }
}
