package com.sigo.programacion.infrastructure.persistence.repository;

import com.sigo.programacion.infrastructure.persistence.entity.EstadoTrasladoPlaza;
import com.sigo.programacion.infrastructure.persistence.entity.TrasladoPlazaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface TrasladoPlazaRepository extends JpaRepository<TrasladoPlazaEntity, Long> {

    /*
     * Evitar la condición ":fecha is null" en JPQL: PostgreSQL puede recibir
     * el parámetro sin tipo SQL y lanzar SQLState 42P18 (parameter $4).
     * Se utilizan consultas con parámetros siempre tipados por comparación.
     */
    @Query("""
      select x from TrasladoPlazaEntity x
      join fetch x.trabajador t join fetch t.puesto
      join fetch x.origen join fetch x.destino
      order by x.fechaInicio desc, t.nombreCompleto
      """)
    List<TrasladoPlazaEntity> consultarTodos();

    @Query("""
      select x from TrasladoPlazaEntity x
      join fetch x.trabajador t join fetch t.puesto
      join fetch x.origen join fetch x.destino
      where x.origen.id = :plazaId or x.destino.id = :plazaId
      order by x.fechaInicio desc, t.nombreCompleto
      """)
    List<TrasladoPlazaEntity> consultarPlaza(@Param("plazaId") Long plazaId);

    @Query("""
      select x from TrasladoPlazaEntity x
      join fetch x.trabajador t join fetch t.puesto
      join fetch x.origen join fetch x.destino
      where (x.origen.id = :plazaId or x.destino.id = :plazaId)
        and (x.fechaFin is null or x.fechaFin >= :desde)
        and x.fechaInicio <= :hasta
      order by x.fechaInicio desc, t.nombreCompleto
      """)
    List<TrasladoPlazaEntity> consultarPlazaPeriodo(
        @Param("plazaId") Long plazaId,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta
    );

    @Query("""
      select x from TrasladoPlazaEntity x
      where x.trabajador.id in :ids and x.estado = :estado
      """)
    List<TrasladoPlazaEntity> activosDeTrabajadores(@Param("ids") Collection<Long> ids,
         @Param("estado") EstadoTrasladoPlaza estado);

    @Query("""
      select x from TrasladoPlazaEntity x
      join fetch x.trabajador join fetch x.origen join fetch x.destino
      where x.estado = :estado
        and (x.origen.id = :plazaId or x.destino.id = :plazaId)
        and x.fechaInicio <= :hasta
        and (x.fechaFin is null or x.fechaFin >= :desde)
      """)
    List<TrasladoPlazaEntity> activosPorPlazaPeriodo(@Param("plazaId") Long plazaId,
        @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
        @Param("estado") EstadoTrasladoPlaza estado);
}
