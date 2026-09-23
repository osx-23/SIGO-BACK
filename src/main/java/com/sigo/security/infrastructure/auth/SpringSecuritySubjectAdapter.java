package com.sigo.security.infrastructure.auth;

import com.sigo.security.application.port.out.SujetoAutenticadoPort;
import com.sigo.security.domain.AutenticacionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecuritySubjectAdapter
        implements SujetoAutenticadoPort {

    @Override
    public Integer requireCodigo() {
        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.UNAUTHORIZED,
                    "Usuario no autenticado"
            );
        }

        try {
            return Integer.valueOf(auth.getName());
        } catch (NumberFormatException exception) {
            throw new AutenticacionException(
                    AutenticacionException.Tipo.UNAUTHORIZED,
                    "Token inválido"
            );
        }
    }
}
