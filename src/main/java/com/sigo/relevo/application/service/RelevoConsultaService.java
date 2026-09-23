package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.out.RelevoConsultaPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelevoConsultaService
        implements ConsultarRelevosUseCase {

    private final RelevoConsultaPort consultaPort;

    @Override
    @Transactional(readOnly = true)
    public List<Elemento> listarElementos() {
        return consultaPort.listarElementos();
    }

    @Override
    @Transactional(readOnly = true)
    public Relevo obtener(Long id) {
        return consultaPort.obtener(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Relevo> listar(
            LocalDate inicio,
            LocalDate fin
    ) {
        LocalDate desde = inicio != null
                ? inicio
                : LocalDate.now().minusDays(1);

        LocalDate hasta = fin != null
                ? fin
                : LocalDate.now();

        if (hasta.isBefore(desde)) {
            throw new BusinessException(
                    "La fecha fin no puede ser menor que la fecha inicio"
            );
        }

        return consultaPort.listar(desde, hasta);
    }
}
