package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.MotivoAusenciaCatalogoUseCase;
import com.sigo.asistencia.application.port.out.MotivoAusenciaCatalogoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotivoAusenciaCatalogoService
        implements MotivoAusenciaCatalogoUseCase {

    private final MotivoAusenciaCatalogoPort port;

    @Override
    @Transactional(readOnly = true)
    public List<MotivoItem> listar() {
        return port.listar();
    }
}
