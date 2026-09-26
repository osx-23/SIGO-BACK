package com.sigo.personal.infrastructure.persistence.adapter;

import com.sigo.personal.application.port.in.TrabajadorUseCase;
import com.sigo.personal.application.port.out.TrabajadorGestionPort;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.RolSistema;
import com.sigo.personal.infrastructure.persistence.repository.PlazaRepository;
import com.sigo.personal.infrastructure.persistence.repository.PuestoRepository;
import com.sigo.personal.infrastructure.persistence.repository.TrabajadorRepository;
import com.sigo.shared.exception.BusinessException;
import com.sigo.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class TrabajadorGestionJpaAdapter
        implements TrabajadorGestionPort {

    private final TrabajadorRepository trabajadorRepository;
    private final PlazaRepository plazaRepository;
    private final PuestoRepository puestoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarAgentesPorPlaza(
            Long plazaId
    ) {
        return trabajadorRepository
                .findAgentesByPlaza(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarControladoresPorPlaza(
            Long plazaId
    ) {
        return trabajadorRepository
                .findControladoresByPlaza(plazaId)
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.TrabajadorData> listarAdministracion(
            Long plazaId
    ) {
        List<Trabajador> trabajadores =
                plazaId == null
                        ? trabajadorRepository.findAllAdminOptimizado()
                        : trabajadorRepository.findAllAdminByPlazaOptimizado(
                                plazaId
                        );

        return trabajadores
                .stream()
                .map(this::toData)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrabajadorUseCase.PuestoData> listarPuestosAdministrables() {
        return puestoRepository
                .findAllByOrderByNombreAsc()
                .stream()
                .map(puesto ->
                        new TrabajadorUseCase.PuestoData(
                                puesto.getId(),
                                puesto.getNombre()
                        )
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCodigo(Integer codigo) {
        return trabajadorRepository.findByCodigo(codigo).isPresent();
    }

    @Override
    @Transactional
    public TrabajadorUseCase.TrabajadorData crearUsuario(
            Integer codigo,
            String nombreCompleto,
            Long puestoId,
            Long plazaId,
            String passwordInicial
    ) {
        Plaza plaza =
                requirePlazaActiva(plazaId);

        Puesto puesto =
                puestoRepository.findById(puestoId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Puesto no encontrado"
                                )
                        );

        RolSistema rolSistema =
                resolverRolPorPuesto(puesto);

        if (trabajadorRepository.findByCodigo(codigo).isPresent()) {
            throw new BusinessException(
                    "Ya existe un trabajador con el código " + codigo
            );
        }

        Trabajador trabajador =
                new Trabajador();

        trabajador.setCodigo(codigo);
        trabajador.setNombreCompleto(
                nombreCompleto.trim()
        );
        trabajador.setPuesto(puesto);
        trabajador.setPlaza(plaza);
        trabajador.setRolSistema(
                rolSistema
        );
        trabajador.setPasswordHash(
                passwordEncoder.encode(
                        passwordInicial
                )
        );
        trabajador.setRequiereCambioPassword(
                true
        );
        trabajador.setActivo(true);

        return toData(
                trabajadorRepository.saveAndFlush(
                        trabajador
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PreparacionActualizacion prepararActualizacion(
            Long trabajadorId,
            Long nuevaPlazaId,
            Long nuevoPuestoId,
            Boolean activo
    ) {
        Trabajador trabajador = requireTrabajador(trabajadorId);
        Plaza nuevaPlaza = requirePlazaActiva(nuevaPlazaId);
        Puesto nuevoPuesto =
                puestoRepository.findById(nuevoPuestoId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Puesto no encontrado"
                                )
                        );

        RolSistema nuevoRol =
                resolverRolPorPuesto(nuevoPuesto);

        Long plazaAnteriorId =
                trabajador.getPlaza() == null
                        ? null
                        : trabajador.getPlaza().getId();

        Long puestoAnteriorId =
                trabajador.getPuesto() == null
                        ? null
                        : trabajador.getPuesto().getId();

        return new PreparacionActualizacion(
                trabajador.getId(),
                plazaAnteriorId,
                nuevaPlaza.getId(),
                !Objects.equals(
                        plazaAnteriorId,
                        nuevaPlaza.getId()
                ),
                !Objects.equals(
                        puestoAnteriorId,
                        nuevoPuesto.getId()
                ),
                !Boolean.TRUE.equals(activo),
                nuevoRol == RolSistema.OPERADOR
        );
    }

    @Override
    @Transactional
    public TrabajadorUseCase.TrabajadorData aplicarActualizacion(
            Long trabajadorId,
            Long nuevaPlazaId,
            Long nuevoPuestoId,
            Boolean activo
    ) {
        Trabajador trabajador = requireTrabajador(trabajadorId);
        Plaza nuevaPlaza = requirePlazaActiva(nuevaPlazaId);
        Puesto nuevoPuesto =
                puestoRepository.findById(nuevoPuestoId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Puesto no encontrado"
                                )
                        );

        trabajador.setPlaza(nuevaPlaza);
        trabajador.setPuesto(nuevoPuesto);
        trabajador.setRolSistema(
                resolverRolPorPuesto(nuevoPuesto)
        );
        trabajador.setActivo(activo);

        return toData(
                trabajadorRepository.saveAndFlush(trabajador)
        );
    }

    private RolSistema resolverRolPorPuesto(Puesto puesto) {
        String nombre =
                puesto == null || puesto.getNombre() == null
                        ? ""
                        : puesto.getNombre()
                                .trim()
                                .toLowerCase();

        if (nombre.startsWith("agente de recaud")) {
            return RolSistema.OPERADOR;
        }

        if (nombre.startsWith("controlador")) {
            return RolSistema.CONTROLADOR;
        }

        if (nombre.equals("supervisor")) {
            return RolSistema.SUPERVISOR;
        }

        throw new BusinessException(
                "El puesto seleccionado no tiene un rol de sistema válido"
        );
    }

    private Trabajador requireTrabajador(Long trabajadorId) {
        return trabajadorRepository
                .findById(trabajadorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trabajador no encontrado"
                        )
                );
    }

    private Plaza requirePlazaActiva(Long plazaId) {
        return plazaRepository
                .findById(plazaId)
                .filter(plaza ->
                        Boolean.TRUE.equals(plaza.getActivo())
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Plaza activa no encontrada"
                        )
                );
    }

    private TrabajadorUseCase.TrabajadorData toData(
            Trabajador trabajador
    ) {
        return new TrabajadorUseCase.TrabajadorData(
                trabajador.getId(),
                trabajador.getCodigo(),
                trabajador.getNombreCompleto(),
                toPuesto(trabajador.getPuesto()),
                toPlaza(trabajador.getPlaza()),
                trabajador.getRolSistema() == null
                        ? null
                        : trabajador.getRolSistema().name(),
                trabajador.getRequiereCambioPassword(),
                trabajador.getActivo()
        );
    }

    private TrabajadorUseCase.PuestoData toPuesto(
            Puesto puesto
    ) {
        return puesto == null
                ? null
                : new TrabajadorUseCase.PuestoData(
                        puesto.getId(),
                        puesto.getNombre()
                );
    }

    private TrabajadorUseCase.PlazaData toPlaza(
            Plaza plaza
    ) {
        return plaza == null
                ? null
                : new TrabajadorUseCase.PlazaData(
                        plaza.getId(),
                        plaza.getCodigo(),
                        plaza.getDescripcion(),
                        plaza.getActivo()
                );
    }
}
