package com.sigo.security.application.port.out;

public interface PasswordSeguridadPort {

    boolean matches(
            String raw,
            String encoded
    );

    String encode(String raw);
}
