package com.sigo.shared.scheduling;

import com.sigo.inventario.infrastructure.persistence.repository.InventarioConteoRepository;
import com.sigo.relevo.application.port.out.RelevoSesionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SesionTurnoVencimientoJob {

    private final InventarioConteoRepository inventarios;
    private final RelevoSesionPort relevos;
    private final JdbcTemplate jdbc;

    /**
     * Una comprobacion por minuto, independiente de que el usuario vuelva a entrar.
     * Si la migracion de relevo_sesiones aun no fue aplicada, solo se limpia Inventario.
     */
    @Scheduled(initialDelayString = "${sigo.vencimiento.initial-delay-ms:5000}",
               fixedDelayString = "${sigo.vencimiento.interval-ms:60000}")
    @Transactional
    public void anularSesionesVencidas() {
        inventarios.anularVencidos();
        Boolean tablaRelevosExiste = jdbc.queryForObject(
                "SELECT to_regclass('relevo_sesiones') IS NOT NULL", Boolean.class);
        if (Boolean.TRUE.equals(tablaRelevosExiste)) {
            relevos.anularVencidos();
        }
    }
}
