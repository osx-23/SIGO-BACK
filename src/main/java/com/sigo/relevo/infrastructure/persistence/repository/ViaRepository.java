package com.sigo.relevo.infrastructure.persistence.repository;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ViaRepository extends JpaRepository<Via,Long> {
 List<Via> findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(Long plazaId);
}
