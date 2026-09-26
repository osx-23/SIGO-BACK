package com.sigo.incidencia.infrastructure.persistence.entity;

import com.sigo.incidencia.domain.EstadoIncidencia;
import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import com.sigo.personal.infrastructure.persistence.entity.Turno;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "incidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private Turno turno;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Trabajador registradoPor;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "tipo_id", nullable = false)
    private TipoIncidencia tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "via_id")
    private Via via;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoIncidencia estado = EstadoIncidencia.OBSERVACION;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_atendido")
    private OffsetDateTime fechaAtendido;
}
