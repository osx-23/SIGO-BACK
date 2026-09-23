package com.sigo.personal.application.service;

import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;
import com.sigo.personal.application.port.out.CatalogoPersonalQueryPort;
import com.sigo.shared.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoPersonalService
        implements CatalogoPersonalUseCase {

    private final CatalogoPersonalQueryPort queryPort;

    @Override
    @Cacheable(CacheConfig.PLAZAS)
    @Transactional(readOnly = true)
    public List<PlazaItem> plazas() {
        return queryPort.plazasActivas();
    }

    @Override
    @Cacheable(CacheConfig.TURNOS)
    @Transactional(readOnly = true)
    public List<TurnoItem> turnos() {
        return queryPort.turnos();
    }
}
