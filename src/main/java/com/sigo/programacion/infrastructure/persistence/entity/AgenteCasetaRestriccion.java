package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "agente_caseta_restriccion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"trabajador_id", "ubicacion_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgenteCasetaRestriccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private ProgramacionUbicacion ubicacion;

    @Column(length = 250)
    private String motivo;

    @Column(nullable = false)
    private Boolean activo = true;
}
