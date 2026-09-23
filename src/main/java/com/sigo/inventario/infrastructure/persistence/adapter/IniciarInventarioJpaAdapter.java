package com.sigo.inventario.infrastructure.persistence.adapter;

import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.out.IniciarInventarioPort;
import com.sigo.inventario.domain.InventarioEstado;
import com.sigo.inventario.infrastructure.persistence.entity.EstadoInventario;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioConteo;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioRol;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoDetalleRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.inventario.infrastructure.persistence.repository.InventarioRolRepository;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IniciarInventarioJpaAdapter
        implements IniciarInventarioPort {

    private final InventarioConteoRepository conteoRepository;
    private final InventarioConteoDetalleRepository detalleRepository;
    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;
    private final InventarioRolRepository rolRepository;

    @Override
    public Optional<Long> inventarioEnProcesoId(
            Long responsableId
    ) {
        return conteoRepository
                .buscarEnProceso(
                        responsableId,
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .map(InventarioConteo::getId);
    }

    @Override
    public IniciarInventarioUseCase.Resumen crear(
            IniciarInventarioUseCase.Usuario usuario
    ) {
        Trabajador trabajador = trabajadorRepository
                .findById(usuario.trabajadorId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trabajador no encontrado"
                        )
                );

        Plaza plaza = plazaRepository
                .findById(usuario.plazaId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza no encontrada"
                        )
                );

        InventarioRol rol = rolRepository
                .findById(usuario.rolId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Rol de inventario no encontrado"
                        )
                );

        InventarioConteo inventario =
                new InventarioConteo();

        inventario.setPlaza(plaza);
        inventario.setResponsable(trabajador);
        inventario.setRol(rol);
        inventario.setEstado(EstadoInventario.EN_PROCESO);

        inventario =
                conteoRepository.save(inventario);

        return new IniciarInventarioUseCase.Resumen(
                inventario.getId(),
                plaza.getId(),
                plaza.getCodigo(),
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                rol.getCodigo(),
                inventario.getFechaInicio(),
                inventario.getFechaFinalizacion(),
                InventarioEstado.EN_PROCESO,
                detalleRepository.countByInventarioId(
                        inventario.getId()
                )
        );
    }
}
