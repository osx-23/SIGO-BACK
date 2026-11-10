package com.sigo.security.infrastructure.jwt;

import com.sigo.security.application.port.out.UsuarioSeguridadPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CambioPasswordFilter extends OncePerRequestFilter {
    private final UsuarioSeguridadPort usuarios;

    public CambioPasswordFilter(UsuarioSeguridadPort usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt && jwt.isAuthenticated()) {
            int codigo;
            try {
                codigo = Integer.parseInt(jwt.getName());
            } catch (NumberFormatException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            var usuario = usuarios.buscarPorCodigo(codigo).filter(u -> u.estaActivo()).orElse(null);
            if (usuario == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String path = request.getServletPath();
            boolean autorizado = ("GET".equals(request.getMethod()) && "/api/auth/me".equals(path))
                    || ("PUT".equals(request.getMethod()) && "/api/auth/change-password".equals(path));
            if (Boolean.TRUE.equals(usuario.requiereCambioPassword()) && !autorizado) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
