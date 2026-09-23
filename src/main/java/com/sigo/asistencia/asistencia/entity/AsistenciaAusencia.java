package com.sigo.asistencia.asistencia.entity;

import com.sigo.asistencia.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "asistencia_ausencia",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_asistencia_trabajador",
                columnNames = {"asistencia_id", "trabajador_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaAusencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asistencia_id", nullable = false)
    private AsistenciaRegistro asistencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "motivo_id", nullable = false)
    private MotivoAusencia motivo;

    @Column(length = 255)
    private String observacion;
}