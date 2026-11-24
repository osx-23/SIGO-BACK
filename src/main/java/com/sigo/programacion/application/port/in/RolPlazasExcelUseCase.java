package com.sigo.programacion.application.port.in;

import java.time.LocalDate;
import java.util.List;

/** Importa exclusivamente la plaza seleccionada desde un rol multipestaña P4/P5. */
public interface RolPlazasExcelUseCase {
    record Persona(int codigo, String nombre, Long trabajadorId, String grupo,
                   int orden, Integer controladorCodigo, Long controladorId,
                   String tipo, String estado, String motivo) {}
    record Turno(Long trabajadorId, int codigo, LocalDate fecha,
                 String anterior, String nuevo, String origen) {}
    record Semana(LocalDate desde, LocalDate hasta) {}
    record VistaPrevia(String plazaCodigo, int anio, int mes,
                       int hojasIgnoradas, int celdasLeidas, int coinciden,
                       List<Persona> personal, List<Turno> cambios,
                       List<Semana> semanasPartTime, List<String> advertencias) {}
    record Resultado(int turnos, int secuencias, int lideres, int omitidos, String mensaje) {}

    /** Clasifica el Excel sin efectuar escrituras: rol operativo con varias plazas o turnos estándar. */
    record FormatoDetectado(String formato) {}
    FormatoDetectado detectarFormato(byte[] archivo);

    VistaPrevia previsualizar(Long plazaId, int anio, int mes, byte[] archivo);
    Resultado confirmar(Long plazaId, int anio, int mes, byte[] archivo);
}
