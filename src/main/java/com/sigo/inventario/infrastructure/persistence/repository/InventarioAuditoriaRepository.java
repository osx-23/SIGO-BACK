package com.sigo.inventario.infrastructure.persistence.repository;
import com.sigo.inventario.infrastructure.persistence.entity.InventarioAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventarioAuditoriaRepository extends JpaRepository<InventarioAuditoria,Long>{}
