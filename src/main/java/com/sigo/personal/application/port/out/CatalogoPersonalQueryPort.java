package com.sigo.personal.application.port.out;

import com.sigo.personal.application.port.in.CatalogoPersonalUseCase;

import java.util.List;

public interface CatalogoPersonalQueryPort {

    List<CatalogoPersonalUseCase.PlazaItem> plazasActivas();

    List<CatalogoPersonalUseCase.TurnoItem> turnos();
}
