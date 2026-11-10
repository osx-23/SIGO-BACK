package com.sigo.security.infrastructure.jwt;

import com.sigo.security.domain.UsuarioSeguridad;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EstadoSesion {
    private final SecretKey key;

    public EstadoSesion(SecretKey key) {
        this.key = key;
    }

    public String huella(UsuarioSeguridad usuario) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            String estado = usuario.id() + "|" + usuario.codigo() + "|" +
                    usuario.passwordHash() + "|" + usuario.rol() + "|" +
                    usuario.plazaId();
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(estado.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo validar la sesión", ex);
        }
    }
}
