package com.sigo.asistencia.security.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class ProductionSafetyGuard implements ApplicationRunner {

    private final Environment environment;

    @Value("${app.test-bootstrap.enabled:false}")
    private boolean testBootstrapEnabled;

    @Value("${app.environment:development}")
    private String appEnvironment;

    @Override
    public void run(ApplicationArguments args) {
        boolean perfilProduccion = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));

        boolean entornoProduccion = "prod".equalsIgnoreCase(appEnvironment)
                || "production".equalsIgnoreCase(appEnvironment);

        if (testBootstrapEnabled && (perfilProduccion || entornoProduccion)) {
            throw new IllegalStateException(
                    "Configuración insegura: app.test-bootstrap.enabled no puede estar activo en producción"
            );
        }
    }
}
