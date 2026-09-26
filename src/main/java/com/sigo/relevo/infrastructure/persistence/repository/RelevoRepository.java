package com.sigo.relevo.infrastructure.persistence.repository;

import com.sigo.relevo.infrastructure.persistence.entity.Relevo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RelevoRepository
        extends JpaRepository<Relevo, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "plaza",
            "turno",
            "operador"
    })
    Optional<Relevo> findById(Long id);

    @EntityGraph(attributePaths = {
            "plaza",
            "turno",
            "operador"
    })
    List<Relevo> findByFechaBetweenOrderByFechaDescHoraDesc(
            LocalDate inicio,
            LocalDate fin
    );
}
