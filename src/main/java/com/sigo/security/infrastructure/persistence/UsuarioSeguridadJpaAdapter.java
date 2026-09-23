package com.sigo.security.infrastructure.persistence;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.security.application.port.out.UsuarioSeguridadPort;
import com.sigo.security.domain.RolSeguridad;
import com.sigo.security.domain.UsuarioSeguridad;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioSeguridadJpaAdapter
        implements UsuarioSeguridadPort {

    private final TrabajadorRepository trabajadorRepository;

    @Override
    public Optional<UsuarioSeguridad> buscarPorCodigo(
            Integer codigo
    ) {
        return trabajadorRepository
                .findByCodigo(codigo)
                .map(this::toDomain);
    }

    @Override
    public Optional<UsuarioSeguridad> buscarPorId(Long id) {
        return trabajadorRepository
                .findById(id)
                .map(this::toDomain);
    }

    @Override
    public void actualizarPassword(
            Long trabajadorId,
            String passwordHash,
            boolean requiereCambioPassword
    ) {
        Trabajador trabajador = trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow();

        trabajador.setPasswordHash(passwordHash);
        trabajador.setRequiereCambioPassword(
                requiereCambioPassword
        );

        trabajadorRepository.save(trabajador);
    }

    private UsuarioSeguridad toDomain(
            Trabajador trabajador
    ) {
        return new UsuarioSeguridad(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                RolSeguridad.from(
                        trabajador.getRolSistema().name()
                ),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId(),
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getCodigo(),
                trabajador.getPasswordHash(),
                trabajador.getRequiereCambioPassword(),
                trabajador.getActivo()
        );
    }
}
