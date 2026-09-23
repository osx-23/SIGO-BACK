package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.Relevo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface RelevoRepository extends JpaRepository<Relevo,Long> {
 List<Relevo> findByFechaBetweenOrderByFechaDescHoraDesc(LocalDate inicio, LocalDate fin);
}
