package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.programacion.domain.GrupoFlujoCaseta;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "configuracion_caseta",
        uniqueConstraints = @UniqueConstraint(columnNames = "ubicacion_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionCaseta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ubicacion_id", nullable = false)
    private ProgramacionUbicacion ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "grupo_flujo", nullable = false, length = 30)
    private GrupoFlujoCaseta grupoFlujo = GrupoFlujoCaseta.SIN_CLASIFICAR;

    @Column(name = "max_semana")
    private Integer maxSemana;

    @Column(name = "max_mes")
    private Integer maxMes;
}
