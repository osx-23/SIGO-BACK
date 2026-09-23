package com.sigo.asistencia.application.service;

import com.sigo.asistencia.application.port.in.MotivoAusenciaCatalogoUseCase;
import com.sigo.asistencia.application.port.out.MotivoAusenciaCatalogoPort;
import com.sigo.shared.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotivoAusenciaCatalogoService
        implements MotivoAusenciaCatalogoUseCase {

    private final MotivoAusenciaCatalogoPort port;

    @Override
    @Cacheable(CacheConfig.MOTIVOS_AUSENCIA)
    @Transactional(readOnly = true)
    public List<MotivoItem> listar() {
        return port.listar();
    }
}
