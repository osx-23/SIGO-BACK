package com.sigo.relevo.application.service;

import com.sigo.relevo.application.port.in.ConsultarRelevosUseCase;
import com.sigo.relevo.application.port.in.GestionarRelevoUseCase;
import com.sigo.relevo.application.port.out.RelevoGestionPort;
import com.sigo.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GestionarRelevoService
        implements GestionarRelevoUseCase {

    private final RelevoGestionPort gestionPort;
    private final ConsultarRelevosUseCase consultaUseCase;

    @Override
    @Transactional
    public ConsultarRelevosUseCase.Relevo registrar(
            Command command
    ) {
        Command validado = validar(command);
        Long id = gestionPort.registrar(validado);
        return consultaUseCase.obtener(id);
    }

    @Override
    @Transactional
    public ConsultarRelevosUseCase.Relevo actualizar(
            Long id,
            Command command
    ) {
        Command validado = validar(command);
        Long actualizado = gestionPort.actualizar(id, validado);
        return consultaUseCase.obtener(actualizado);
    }

    private Command validar(Command command) {
        if (command == null
                || command.plazaId() == null
                || command.turnoId() == null
                || command.operadorId() == null
                || command.fecha() == null
                || command.hora() == null
                || command.checklist() == null
                || command.checklist().isEmpty()) {
            throw new BusinessException(
                    "Los datos del relevo son obligatorios"
            );
        }

        Map<Long, RelevoGestionPort.ElementoConfig> activos =
                new HashMap<>();

        for (RelevoGestionPort.ElementoConfig elemento
                : gestionPort.elementosActivos()) {
            activos.put(elemento.id(), elemento);
        }

        Set<Long> checklistIds = new HashSet<>();

        for (ChecklistItem item : command.checklist()) {
            if (item == null
                    || item.elementoId() == null
                    || item.estado() == null) {
                throw new BusinessException(
                        "El checklist contiene datos incompletos"
                );
            }

            if (!checklistIds.add(item.elementoId())) {
                throw new BusinessException(
                        "No puedes registrar dos veces el mismo elemento"
                );
            }

            RelevoGestionPort.ElementoConfig elemento =
                    activos.get(item.elementoId());

            if (elemento == null) {
                throw new BusinessException(
                        "El elemento "
                                + item.elementoId()
                                + " no existe o está inactivo"
                );
            }

            validarDetalle(
                    item.estado(),
                    item.detalle(),
                    elemento.nombre()
            );

            if (Boolean.TRUE.equals(elemento.requiereCantidad())
                    && item.cantidad() == null) {
                throw new BusinessException(
                        "Debes indicar la cantidad para "
                                + elemento.nombre()
                );
            }
        }

        if (checklistIds.size() != activos.size()
                || !checklistIds.containsAll(activos.keySet())) {
            throw new BusinessException(
                    "Debes registrar todos los elementos activos del checklist"
            );
        }

        List<ViaItem> vias =
                command.vias() == null
                        ? List.of()
                        : command.vias();

        Set<Long> viaIds = new HashSet<>();

        for (ViaItem item : vias) {
            if (item == null
                    || item.viaId() == null
                    || item.estado() == null) {
                throw new BusinessException(
                        "El reporte de vías contiene datos incompletos"
                );
            }

            if (!viaIds.add(item.viaId())) {
                throw new BusinessException(
                        "No puedes registrar la misma vía dos veces"
                );
            }

            RelevoGestionPort.ViaConfig via =
                    gestionPort.requireVia(item.viaId());

            if (!Boolean.TRUE.equals(via.activa())) {
                throw new BusinessException(
                        "La vía " + via.numero() + " está inactiva"
                );
            }

            if (!command.plazaId().equals(via.plazaId())) {
                throw new BusinessException(
                        "La vía "
                                + via.numero()
                                + " no pertenece a la plaza seleccionada"
                );
            }

            validarDetalle(
                    item.estado(),
                    item.detalle(),
                    "Vía " + via.numero()
            );
        }

        return new Command(
                command.plazaId(),
                command.turnoId(),
                command.operadorId(),
                command.fecha(),
                command.hora(),
                limpiar(command.observaciones()),
                limpiar(command.resumen()),
                command.checklist()
                        .stream()
                        .map(item ->
                                new ChecklistItem(
                                        item.elementoId(),
                                        item.estado(),
                                        limpiar(item.detalle()),
                                        item.cantidad()
                                )
                        )
                        .toList(),
                vias.stream()
                        .map(item ->
                                new ViaItem(
                                        item.viaId(),
                                        item.estado(),
                                        limpiar(item.detalle())
                                )
                        )
                        .toList()
        );
    }

    private void validarDetalle(
            com.sigo.relevo.domain.EstadoRelevo estado,
            String detalle,
            String nombre
    ) {
        if (estado.requiereDetalle()
                && (detalle == null || detalle.isBlank())) {
            throw new BusinessException(
                    "Debes indicar un detalle para "
                            + nombre
                            + " cuando el estado es "
                            + estado
            );
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
