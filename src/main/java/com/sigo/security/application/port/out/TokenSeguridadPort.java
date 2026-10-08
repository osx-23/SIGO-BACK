package com.sigo.security.application.port.out;

import com.sigo.security.domain.UsuarioSeguridad;

public interface TokenSeguridadPort {

    String generar(UsuarioSeguridad usuario);

    String generarAvi(UsuarioSeguridad usuario);

    long expirationSeconds();

    long aviExpirationSeconds();
}
