package com.sigo.inventario.application.service;

import com.sigo.inventario.application.port.in.IniciarInventarioUseCase;
import com.sigo.inventario.application.port.out.IniciarInventarioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class IniciarInventarioService
        implements IniciarInventarioUseCase {

    private final IniciarInventarioPort iniciarPort;
    private final InventarioAuditoriaService auditoria;

    @Override
    @Transactional
    public Resumen iniciar(Usuario usuario) {
        if (usuario == null
                || usuario.trabajadorId() == null
                || usuario.plazaId() == null
                || usuario.rolId() == null
                || usuario.rolCodigo() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Trabajador sin plaza o rol de inventario asignado"
            );
        }

        iniciarPort
                .inventarioEnProcesoId(
                        usuario.trabajadorId()
                )
                .ifPresent(id -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Ya existe un inventario EN_PROCESO: "
                                    + id
                    );
                });

        Resumen resumen =
                iniciarPort.crear(usuario);

        auditoria.registrar(
                usuario.trabajadorId(),
                "INVENTARIO_INICIADO",
                "INVENTARIO_CONTEO",
                resumen.id(),
                Map.of(
                        "plaza",
                        resumen.plaza(),
                        "rol",
                        usuario.rolCodigo()
                )
        );

        return resumen;
    }
}
