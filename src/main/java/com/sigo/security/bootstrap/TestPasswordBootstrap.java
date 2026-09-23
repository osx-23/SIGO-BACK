package com.sigo.security.bootstrap;

import com.sigo.personal.entity.Trabajador;
import com.sigo.personal.repository.TrabajadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "app.test-bootstrap.enabled",
        havingValue = "true"
)
public class TestPasswordBootstrap implements ApplicationRunner {

    private final TrabajadorRepository trabajadorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.test-bootstrap.operador.codigo:2396}")
    private Integer operadorCodigo;

    @Value("${app.test-bootstrap.operador.password}")
    private String operadorPassword;

    @Value("${app.test-bootstrap.controlador.codigo:287}")
    private Integer controladorCodigo;

    @Value("${app.test-bootstrap.controlador.password}")
    private String controladorPassword;

    @Value("${app.test-bootstrap.supervisor.codigo:550}")
    private Integer supervisorCodigo;

    @Value("${app.test-bootstrap.supervisor.password}")
    private String supervisorPassword;

    @Value("${app.test-bootstrap.password-inicial:12345}")
    private String passwordInicial;

    @Override
    public void run(ApplicationArguments args) {

        // 1. Mantiene los 3 usuarios especiales de prueba
        inicializarUsuarioPrueba(
                operadorCodigo,
                operadorPassword
        );

        inicializarUsuarioPrueba(
                controladorCodigo,
                controladorPassword
        );

        inicializarUsuarioPrueba(
                supervisorCodigo,
                supervisorPassword
        );

        // 2. Inicializa todos los demás usuarios sin contraseña
        inicializarUsuariosPendientes();
    }

    private void inicializarUsuarioPrueba(
            Integer codigo,
            String passwordPlano
    ) {

        if (passwordPlano == null || passwordPlano.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar la contraseña de prueba para el código " + codigo
            );
        }

        Trabajador trabajador = trabajadorRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el trabajador de prueba con código " + codigo
                ));

        if (
                trabajador.getPasswordHash() == null ||
                        trabajador.getPasswordHash().isBlank()
        ) {

            trabajador.setPasswordHash(
                    passwordEncoder.encode(passwordPlano)
            );
        }

        /*
         * Los usuarios especiales de prueba NO están obligados
         * a cambiar contraseña.
         */
        trabajador.setRequiereCambioPassword(false);

        trabajadorRepository.save(trabajador);

        log.info(
                "Usuario de prueba configurado. Código: {}",
                codigo
        );
    }

    private void inicializarUsuariosPendientes() {

        if (
                passwordInicial == null ||
                        passwordInicial.isBlank()
        ) {
            throw new IllegalStateException(
                    "La contraseña inicial no puede estar vacía"
            );
        }

        List<Trabajador> trabajadores =
                trabajadorRepository.findAll();

        int actualizados = 0;

        for (Trabajador trabajador : trabajadores) {

            /*
             * No tocamos los 3 usuarios especiales
             */
            if (esUsuarioPrueba(trabajador.getCodigo())) {
                continue;
            }

            /*
             * Solo inicializamos usuarios que todavía
             * no tienen contraseña.
             */
            if (
                    trabajador.getPasswordHash() == null ||
                            trabajador.getPasswordHash().isBlank()
            ) {

                trabajador.setPasswordHash(
                        passwordEncoder.encode(passwordInicial)
                );

                trabajador.setRequiereCambioPassword(true);

                trabajadorRepository.save(trabajador);

                actualizados++;
            }
        }

        log.info(
                "{} trabajadores inicializados con contraseña temporal.",
                actualizados
        );
    }

    private boolean esUsuarioPrueba(Integer codigo) {

        return codigo.equals(operadorCodigo)
                || codigo.equals(controladorCodigo)
                || codigo.equals(supervisorCodigo);
    }
}