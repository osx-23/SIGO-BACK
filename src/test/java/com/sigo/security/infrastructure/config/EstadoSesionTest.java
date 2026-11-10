package com.sigo.security.infrastructure.config;

import com.sigo.security.application.port.out.UsuarioSeguridadPort;
import com.sigo.security.domain.RolSeguridad;
import com.sigo.security.domain.UsuarioSeguridad;
import com.sigo.security.infrastructure.jwt.EstadoSesion;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EstadoSesionTest {
    @Test
    void rechazaTokenSiCambiaPasswordRolOCuentaInactiva() {
        var key = new SecretKeySpec("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var huella = new EstadoSesion(key);
        var usuarios = mock(UsuarioSeguridadPort.class);
        var original = usuario("hash1", RolSeguridad.OPERADOR, true);
        when(usuarios.buscarPorCodigo(999)).thenReturn(Optional.of(original));
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        var claims = JwtClaimsSet.builder().issuer("sigo-api").subject("999")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
                .claim("estadoSesion", huella.huella(original)).build();
        var header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        var decoder = new SecurityConfig().jwtDecoder(key, usuarios, huella, "sigo-api");
        assertDoesNotThrow(() -> decoder.decode(token));
        when(usuarios.buscarPorCodigo(999)).thenReturn(Optional.of(usuario("hash2", RolSeguridad.OPERADOR, true)));
        assertThrows(JwtException.class, () -> decoder.decode(token));
        when(usuarios.buscarPorCodigo(999)).thenReturn(Optional.of(usuario("hash1", RolSeguridad.CONTROLADOR, true)));
        assertThrows(JwtException.class, () -> decoder.decode(token));
        when(usuarios.buscarPorCodigo(999)).thenReturn(Optional.of(usuario("hash1", RolSeguridad.OPERADOR, false)));
        assertThrows(JwtException.class, () -> decoder.decode(token));
    }

    private UsuarioSeguridad usuario(String hash, RolSeguridad rol, boolean activo) {
        return new UsuarioSeguridad(1L, 999, "Usuario", rol, 1L, "P", hash, false, activo);
    }
}
