package com.sigo.security.infrastructure.jwt;

import com.sigo.security.application.port.out.TokenSeguridadPort;
import com.sigo.security.domain.ModuloAccesoPolicy;
import com.sigo.security.domain.UsuarioSeguridad;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenSeguridadAdapter
        implements TokenSeguridadPort {

    private final JwtEncoder jwtEncoder;

    private final ModuloAccesoPolicy moduloPolicy =
            new ModuloAccesoPolicy();

    @Value("${app.jwt.issuer:sigo-api}")
    private String issuer;

    @Value("${app.jwt.expiration-seconds:28800}")
    private long expirationSeconds;

    @Override
    public String generar(UsuarioSeguridad usuario) {
        Instant now = Instant.now();

        JwtClaimsSet.Builder builder =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .issuedAt(now)
                        .expiresAt(
                                now.plusSeconds(
                                        expirationSeconds
                                )
                        )
                        .subject(
                                usuario.codigo().toString()
                        )
                        .claim(
                                "trabajadorId",
                                usuario.id()
                        )
                        .claim(
                                "nombre",
                                usuario.nombre()
                        )
                        .claim(
                                "rol",
                                usuario.rol().name()
                        )
                        .claim(
                                "modulos",
                                moduloPolicy.modulosPara(
                                        usuario.rol()
                                )
                        );

        if (usuario.plazaId() != null) {
            builder.claim(
                    "plazaId",
                    usuario.plazaId()
            );
        }

        JwsHeader header =
                JwsHeader
                        .with(MacAlgorithm.HS256)
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                builder.build()
                        )
                )
                .getTokenValue();
    }

    @Override
    public long expirationSeconds() {
        return expirationSeconds;
    }
}
