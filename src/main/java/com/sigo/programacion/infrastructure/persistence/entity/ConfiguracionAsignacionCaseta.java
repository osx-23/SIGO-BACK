package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "configuracion_asignacion_caseta",
        uniqueConstraints = @UniqueConstraint(columnNames = "plaza_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionAsignacionCaseta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plaza_id", nullable = false)
    private Plaza plaza;

    @Column(name = "max_misma_caseta_semana", nullable = false)
    private Integer maxMismaCasetaSemana = 3;

    @Column(name = "max_misma_caseta_mes", nullable = false)
    private Integer maxMismaCasetaMes = 8;

    @Column(name = "max_consecutivos", nullable = false)
    private Integer maxConsecutivos = 2;

    @Column(name = "balancear_flujo", nullable = false)
    private Boolean balancearFlujo = true;
}
