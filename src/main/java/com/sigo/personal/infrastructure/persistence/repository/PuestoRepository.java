package com.sigo.personal.infrastructure.persistence.repository;

import com.sigo.personal.infrastructure.persistence.entity.Puesto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuestoRepository
        extends JpaRepository<Puesto, Long> {

    List<Puesto> findAllByOrderByNombreAsc();
}
