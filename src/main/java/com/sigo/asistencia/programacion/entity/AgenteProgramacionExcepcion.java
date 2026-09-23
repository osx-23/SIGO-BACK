package com.sigo.asistencia.programacion.entity;

import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "agente_programacion_excepcion",
        uniqueConstraints = @UniqueConstraint(
                name = "agente_programacion_excepcion_trabajador_plaza_key",
                columnNames = {"trabajador_id", "plaza_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgenteProgramacionExcepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @Column(name = "permite_a", nullable = false)
    private Boolean permiteA = true;

    @Column(name = "permite_b", nullable = false)
    private Boolean permiteB = true;

    @Column(name = "permite_c", nullable = false)
    private Boolean permiteC = true;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "color", nullable = false, length = 20)
    private String color = "#FFF3B0";

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
