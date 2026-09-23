package com.sigo.programacion.entity;

import com.sigo.personal.entity.Plaza;
import com.sigo.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(
        name = "programacion_secuencia_agente",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_programacion_secuencia_agente_agente",
                        columnNames = "agente_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramacionSecuenciaAgente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "agente_id",
            nullable = false
    )
    private Trabajador agente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "plaza_id",
            nullable = false
    )
    private Plaza plaza;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "grupo",
            length = 20
    )
    private GrupoProgramacion grupo;

    @Column(name = "orden")
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private Trabajador actualizadoPor;

    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private OffsetDateTime updatedAt;
}