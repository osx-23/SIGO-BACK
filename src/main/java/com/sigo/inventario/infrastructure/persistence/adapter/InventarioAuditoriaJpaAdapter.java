package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.out.InventarioAuditoriaPort;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAuditoria;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioAuditoriaRepository;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventarioAuditoriaJpaAdapter
        implements InventarioAuditoriaPort {

    private final InventarioAuditoriaRepository repository;
    private final TrabajadorRepository trabajadorRepository;

    @Override
    public void registrar(
            Long usuarioId,
            String accion,
            String entidad,
            Long entidadId,
            Map<String, Object> detalle
    ) {
        Trabajador usuario = trabajadorRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuario de auditoría no encontrado"
                        )
                );

        InventarioAuditoria auditoria =
                new InventarioAuditoria();

        auditoria.setUsuario(usuario);
        auditoria.setAccion(accion);
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setDetalle(detalle);

        repository.save(auditoria);
    }
}
