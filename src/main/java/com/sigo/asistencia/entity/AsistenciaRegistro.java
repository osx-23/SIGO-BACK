package com.sigo.asistencia.entity;

import com.sigo.personal.entity.Plaza;
import com.sigo.personal.entity.Trabajador;
import com.sigo.personal.entity.Turno;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "asistencia_registro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "plaza_id",
            nullable = false
    )
    private Plaza plaza;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "turno_id",
            nullable = false
    )
    private Turno turno;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "controlador_id",
            nullable = false
    )
    private Trabajador controlador;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private Integer programados;

    @Column(nullable = false)
    private Integer presentes;

    @Column(
            name = "apoyo_solicitado",
            nullable = false
    )
    private Integer apoyoSolicitado = 0;

    @Column(
            name = "detalle_apoyo",
            length = 500
    )
    private String detalleApoyo;

    @Column(
            insertable = false,
            updatable = false
    )
    private BigDecimal porcentaje;

    private String notas;

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