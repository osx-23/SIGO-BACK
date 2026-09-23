package com.sigo.relevo.repository;
import com.sigo.relevo.entity.Via;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ViaRepository extends JpaRepository<Via,Long> {
 List<Via> findByPlazaIdAndActivaTrueOrderByOrdenAscNumeroAsc(Long plazaId);
}
