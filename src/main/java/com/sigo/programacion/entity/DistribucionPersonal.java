package com.sigo.programacion.entity;

import com.sigo.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="distribucion_personal", uniqueConstraints=@UniqueConstraint(columnNames={"programacion_turno_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DistribucionPersonal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="programacion_turno_id", nullable=false)
    private ProgramacionTurno programacionTurno;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="ubicacion_id", nullable=false)
    private ProgramacionUbicacion ubicacion;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="asignado_por", nullable=false)
    private Trabajador asignadoPor;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actualizado_por")
    private Trabajador actualizadoPor;

    @Column(length=500)
    private String observacion;

    @Column(name="created_at", insertable=false, updatable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private OffsetDateTime updatedAt;
}
