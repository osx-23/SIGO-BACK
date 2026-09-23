package com.sigo.security.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(
                        sm -> sm.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth

                        /*
                         * =====================================================
                         * AUTH
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/auth/login",
                                "/error"
                        )
                        .permitAll()


                        /*
                         * =====================================================
                         * PROGRAMACIÓN - MI HORARIO
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/programacion/mi-horario"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR",
                                "OPERADOR"
                        )


                        /*
                         * =====================================================
                         * PROGRAMACIÓN - LÍDERES
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/programacion/grupos/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )


                        /*
                         * =====================================================
                         * PROGRAMACIÓN - SECUENCIAS
                         * =====================================================
                         */

                        /*
                         * Supervisor y Controlador pueden consultar
                         * la organización por secuencias.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/programacion/secuencias"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )

                        /*
                         * Solo Supervisor puede:
                         *
                         * - cambiar de secuencia
                         * - modificar el orden
                         */
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/programacion/secuencias/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )


                        /*
                         * =====================================================
                         * PROGRAMACIÓN - EXCEPCIONES DE AGENTES
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/programacion/excepciones/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/programacion/excepciones/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/programacion/excepciones/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )

                        /*
                         * =====================================================
                         * PROGRAMACIÓN - GENERADOR
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/programacion/generar-propuesta"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )


                        /*
                         * =====================================================
                         * PROGRAMACIÓN - TURNOS
                         * =====================================================
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/programacion/turnos"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/programacion/turnos"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )


                        /*
                         * =====================================================
                         * DISTRIBUCIÓN - CONFIGURACIÓN DE UBICACIONES
                         * =====================================================
                         * Solo Supervisor puede crear, editar, activar o
                         * desactivar casetas/ubicaciones.
                         */

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/distribucion/ubicaciones/configuracion"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/distribucion/ubicaciones"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/distribucion/ubicaciones/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/distribucion/ubicaciones/**"
                        )
                        .hasRole(
                                "SUPERVISOR"
                        )


                        /*
                         * =====================================================
                         * DISTRIBUCIÓN
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/distribucion/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * DASHBOARD
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/dashboard/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * ASISTENCIA
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/asistencias/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * TRABAJADORES
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/trabajadores/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * CHAT
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/chat/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * INVENTARIO - ADMINISTRACIÓN
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/inventario/productos/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )

                        .requestMatchers(
                                "/api/inventario/catalogos/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR"
                        )


                        /*
                         * =====================================================
                         * RELEVOS / VÍAS
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/relevos/**",
                                "/api/vias/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR",
                                "OPERADOR"
                        )


                        /*
                         * =====================================================
                         * INVENTARIO OPERATIVO
                         * =====================================================
                         */

                        .requestMatchers(
                                "/api/inventarios/**",
                                "/api/inventario/me",
                                "/api/inventario/stock/**"
                        )
                        .hasAnyRole(
                                "SUPERVISOR",
                                "CONTROLADOR",
                                "OPERADOR"
                        )


                        /*
                         * =====================================================
                         * CUALQUIER OTRA RUTA
                         * =====================================================
                         */

                        .anyRequest()
                        .authenticated()
                )


                /*
                 * ============================================================
                 * JWT
                 * ============================================================
                 */

                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(
                                        jwt ->
                                                jwt
                                                        .decoder(
                                                                jwtDecoder
                                                        )
                                                        .jwtAuthenticationConverter(
                                                                token -> {

                                                                    String rol =
                                                                            token.getClaimAsString(
                                                                                    "rol"
                                                                            );

                                                                    var authorities =
                                                                            rol == null

                                                                                    ? List.<SimpleGrantedAuthority>of()

                                                                                    : List.of(
                                                                                    new SimpleGrantedAuthority(
                                                                                            "ROLE_" + rol
                                                                                    )
                                                                            );


                                                                    return new JwtAuthenticationToken(
                                                                            token,
                                                                            authorities,
                                                                            token.getSubject()
                                                                    );
                                                                }
                                                        )
                                )
                )
                .build();
    }


    /*
     * ============================================================
     * PASSWORD ENCODER
     * ============================================================
     */

    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }


    /*
     * ============================================================
     * JWT SECRET
     * ============================================================
     */

    @Bean
    SecretKey jwtSecretKey(
            @Value("${app.jwt.secret}")
            String secret
    ) {

        if (
                secret == null
                        ||
                        secret
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                                .length < 32
        ) {

            throw new IllegalStateException(
                    "JWT_SECRET debe tener al menos 32 bytes para HS256"
            );
        }


        return new SecretKeySpec(
                secret.getBytes(
                        StandardCharsets.UTF_8
                ),
                "HmacSHA256"
        );
    }


    /*
     * ============================================================
     * JWT ENCODER
     * ============================================================
     */

    @Bean
    JwtEncoder jwtEncoder(
            SecretKey key
    ) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(
                        key
                )
        );
    }


    /*
     * ============================================================
     * JWT DECODER
     * ============================================================
     */

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey key,

            @Value("${app.jwt.issuer:sigo-api}")
            String issuer
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(
                                key
                        )
                        .macAlgorithm(
                                MacAlgorithm.HS256
                        )
                        .build();


        decoder.setJwtValidator(
                JwtValidators
                        .createDefaultWithIssuer(
                                issuer
                        )
        );


        return decoder;
    }
}