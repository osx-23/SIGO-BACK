package com.sigo.programacion.application.port.out;

public interface ProgramacionAccessPort {

    Long requireSupervisorId();

    void validarLecturaPlaza(Long plazaId);

    void validarGestionPlaza(Long plazaId);
}
