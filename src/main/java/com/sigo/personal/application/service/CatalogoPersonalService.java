package com.sigo.personal.application.service;

import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;
import com.sigo.personal.application.port.out.CatalogoPersonalQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoPersonalService
        implements CatalogoPersonalUseCase {

    private final CatalogoPersonalQueryPort queryPort;

    @Override
    @Transactional(readOnly = true)
    public List<PlazaItem> plazas() {
        return queryPort.plazasActivas();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoItem> turnos() {
        return queryPort.turnos();
    }
}
