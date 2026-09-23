package com.sigo.asistencia.application.port.out;

import com.sigo.asistencia.application.port.in.MotivoAusenciaCatalogoUseCase;

import java.util.List;

public interface MotivoAusenciaCatalogoPort {

    List<MotivoAusenciaCatalogoUseCase.MotivoItem> listar();
}
