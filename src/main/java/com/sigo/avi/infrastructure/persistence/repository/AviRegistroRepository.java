package com.sigo.avi.infrastructure.persistence.repository;

import com.sigo.avi.domain.AviAccion;
import com.sigo.avi.infrastructure.persistence.entity.AviRegistroEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AviRegistroRepository
        extends JpaRepository<AviRegistroEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "plaza"})
    Optional<AviRegistroEntity> findById(UUID id);

    @Modifying
    @Query(
            value = """
                INSERT INTO avi_registros (
                    id,
                    usuario_id,
                    plaza_id,
                    placa,
                    via,
                    accion,
                    fecha_hora_evento,
                    texto_reconocido
                )
                VALUES (
                    :id,
                    :usuarioId,
                    :plazaId,
                    :placa,
                    :via,
                    :accion,
                    :fechaHoraEvento,
                    :textoReconocido
                )
                ON CONFLICT (id) DO NOTHING
                """,
            nativeQuery = true
    )
    int insertarSiAusente(
            @Param("id") UUID id,
            @Param("usuarioId") Long usuarioId,
            @Param("plazaId") Long plazaId,
            @Param("placa") String placa,
            @Param("via") Integer via,
            @Param("accion") String accion,
            @Param("fechaHoraEvento")
            OffsetDateTime fechaHoraEvento,
            @Param("textoReconocido")
            String textoReconocido
    );

    @EntityGraph(attributePaths = {"usuario", "plaza"})
    @Query("""
        SELECT r
        FROM AviRegistroEntity r
        WHERE r.fechaHoraEvento >= :desde
          AND r.fechaHoraEvento <= :hasta
          AND (:plazaId IS NULL OR r.plaza.id = :plazaId)
          AND (:usuarioId IS NULL OR r.usuario.id = :usuarioId)
          AND (:via IS NULL OR r.via = :via)
          AND (:accion IS NULL OR r.accion = :accion)
        ORDER BY r.fechaHoraEvento DESC,
                 r.fechaHoraRecepcion DESC
    """)
    List<AviRegistroEntity> buscar(
            @Param("desde") OffsetDateTime desde,
            @Param("hasta") OffsetDateTime hasta,
            @Param("plazaId") Long plazaId,
            @Param("usuarioId") Long usuarioId,
            @Param("via") Integer via,
            @Param("accion") AviAccion accion
    );
}
