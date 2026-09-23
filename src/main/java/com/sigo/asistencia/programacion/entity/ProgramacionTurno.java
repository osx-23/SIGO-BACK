package com.sigo.asistencia.programacion.entity;

import com.sigo.asistencia.personal.entity.Plaza;
import com.sigo.asistencia.personal.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name="programacion_turno", uniqueConstraints=@UniqueConstraint(columnNames={"trabajador_id","fecha"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProgramacionTurno {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="trabajador_id", nullable=false)
    private Trabajador trabajador;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="plaza_id", nullable=false)
    private Plaza plaza;

    @Column(nullable=false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=3)
    private EstadoProgramacion estado;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="creado_por")
    private Trabajador creadoPor;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="actualizado_por")
    private Trabajador actualizadoPor;

    @Column(name="created_at", insertable=false, updatable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private OffsetDateTime updatedAt;
}
