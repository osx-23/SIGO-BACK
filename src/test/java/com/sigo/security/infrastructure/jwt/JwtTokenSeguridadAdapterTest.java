package com.sigo.security.infrastructure.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sigo.security.domain.RolSeguridad;
import com.sigo.security.domain.UsuarioSeguridad;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenSeguridadAdapterTest {

    @Test
    void generaJwtConClaimsDeSesionYExpiracion() {
        SecretKey key = new SecretKeySpec(
                "01234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        JwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(key)
        );

        JwtTokenSeguridadAdapter adapter =
                new JwtTokenSeguridadAdapter(encoder);

        ReflectionTestUtils.setField(
                adapter,
                "issuer",
                "sigo-api"
        );

        ReflectionTestUtils.setField(
                adapter,
                "expirationSeconds",
                3600L
        );

        UsuarioSeguridad usuario =
                new UsuarioSeguridad(
                        10L,
                        287,
                        "Supervisor Test",
                        RolSeguridad.SUPERVISOR,
                        4L,
                        "P4",
                        "hash",
                        false,
                        true
                );

        String token = adapter.generar(usuario);

        JwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        var jwt = decoder.decode(token);

        assertEquals("287", jwt.getSubject());
        assertEquals("sigo-api", jwt.getIssuer().toString());
        assertEquals(
                "SUPERVISOR",
                jwt.getClaimAsString("rol")
        );
        assertEquals(
                10L,
                jwt.getClaimAsLong("trabajadorId")
        );
        assertEquals(
                "Supervisor Test",
                jwt.getClaimAsString("nombre")
        );
        assertEquals(
                4L,
                jwt.getClaimAsLong("plazaId")
        );

        var modulos = jwt.getClaimAsStringList("modulos");

        assertTrue(modulos.contains("PROGRAMACION"));
        assertTrue(modulos.contains("ASISTENCIA"));
        assertEquals(3600L, adapter.expirationSeconds());

        long duracion =
                jwt.getExpiresAt().getEpochSecond()
                        - jwt.getIssuedAt().getEpochSecond();

        assertEquals(3600L, duracion);
    }

    @Test
    void tokenDeOperadorNoIncluyeModulosAdministrativos() {
        SecretKey key = new SecretKeySpec(
                "01234567890123456789012345678901"
                        .getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        JwtTokenSeguridadAdapter adapter =
                new JwtTokenSeguridadAdapter(
                        new NimbusJwtEncoder(
                                new ImmutableSecret<>(key)
                        )
                );

        ReflectionTestUtils.setField(adapter, "issuer", "sigo-api");
        ReflectionTestUtils.setField(
                adapter,
                "expirationSeconds",
                3600L
        );

        String token = adapter.generar(
                new UsuarioSeguridad(
                        20L,
                        9001,
                        "Operador Test",
                        RolSeguridad.OPERADOR,
                        4L,
                        "P4",
                        "hash",
                        false,
                        true
                )
        );

        JwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        var modulos = decoder
                .decode(token)
                .getClaimAsStringList("modulos");

        assertTrue(modulos.contains("RELEVOS"));
        assertTrue(modulos.contains("INVENTARIO"));
        assertTrue(modulos.contains("MI_HORARIO"));
        assertFalse(modulos.contains("ASISTENCIA"));
        assertFalse(modulos.contains("PROGRAMACION"));
    }
}
