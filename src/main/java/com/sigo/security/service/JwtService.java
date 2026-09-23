package com.sigo.security.service;

import com.sigo.personal.entity.Trabajador;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final ModuloAccesoService moduloAccesoService;

    @Value("${app.jwt.issuer:sigo-api}")
    private String issuer;

    @Value("${app.jwt.expiration-seconds:28800}")
    private long expirationSeconds;

    public String generar(Trabajador trabajador) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .subject(trabajador.getCodigo().toString())
                .claim("trabajadorId", trabajador.getId())
                .claim("nombre", trabajador.getNombreCompleto())
                .claim("rol", trabajador.getRolSistema().name())
                .claim("modulos", moduloAccesoService.modulosPara(trabajador.getRolSistema()));

        if (trabajador.getPlaza() != null) {
            builder.claim("plazaId", trabajador.getPlaza().getId());
        }

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }

    public long expirationSeconds() {
        return expirationSeconds;
    }
}
