package com.sigo.security.infrastructure.persistence;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.security.application.port.in.UsuarioActualUseCase;
import com.sigo.security.application.port.out.UsuarioActualDataPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioActualJpaAdapter
        implements UsuarioActualDataPort {

    private final TrabajadorRepository trabajadorRepository;

    @Override
    public Optional<UsuarioActualUseCase.UsuarioActual>
    buscarActivoPorCodigo(Integer codigo) {
        return trabajadorRepository
                .findByCodigo(codigo)
                .filter(t ->
                        Boolean.TRUE.equals(t.getActivo())
                )
                .map(this::toData);
    }

    private UsuarioActualUseCase.UsuarioActual toData(
            Trabajador trabajador
    ) {
        return new UsuarioActualUseCase.UsuarioActual(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                trabajador.getRolSistema().name(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getCodigo(),
                trabajador.getPuesto() == null
                        ? null
                        : trabajador.getPuesto().getId(),
                trabajador.getPuesto() == null
                        ? null
                        : trabajador.getPuesto().getNombre()
        );
    }
}
