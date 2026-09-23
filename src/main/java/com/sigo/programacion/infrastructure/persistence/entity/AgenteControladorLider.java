package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name="agente_controlador_lider")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AgenteControladorLider {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="agente_id", nullable=false)
    private Trabajador agente;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="controlador_id", nullable=false)
    private Trabajador controlador;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="plaza_id", nullable=false)
    private Plaza plaza;

    @Column(name="fecha_inicio", nullable=false)
    private LocalDate fechaInicio;

    @Column(name="fecha_fin")
    private LocalDate fechaFin;

    @Column(nullable=false)
    private Boolean activo=true;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="asignado_por")
    private Trabajador asignadoPor;

    @Column(name="created_at", insertable=false, updatable=false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at", insertable=false, updatable=false)
    private OffsetDateTime updatedAt;
}
