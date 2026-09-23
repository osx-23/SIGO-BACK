package com.sigo.security.infrastructure.crypto;

import com.sigo.security.application.port.out.PasswordSeguridadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordSeguridadAdapter
        implements PasswordSeguridadPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(
            String raw,
            String encoded
    ) {
        return passwordEncoder.matches(raw, encoded);
    }

    @Override
    public String encode(String raw) {
        return passwordEncoder.encode(raw);
    }
}
