package com.sigo.security.application.service;

import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.security.application.port.out.SujetoAutenticadoPort;
import com.sigo.security.application.port.out.UsuarioActualDataPort;
import com.sigo.security.domain.AutenticacionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioActualService
        implements UsuarioActualUseCase {

    private final SujetoAutenticadoPort sujetoPort;
    private final UsuarioActualDataPort usuarioDataPort;

    @Override
    public UsuarioActual requireActual() {
        Integer codigo = sujetoPort.requireCodigo();

        return usuarioDataPort
                .buscarActivoPorCodigo(codigo)
                .orElseThrow(() ->
                        new AutenticacionException(
                                AutenticacionException.Tipo.UNAUTHORIZED,
                                "Usuario no encontrado o inactivo"
                        )
                );
    }
}
