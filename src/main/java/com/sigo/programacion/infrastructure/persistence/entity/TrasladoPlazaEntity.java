package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.personal.infrastructure.persistence.entity.Trabajador;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Table(name = "traslados_programados_plaza")
@Getter @Setter
public class TrasladoPlazaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plaza_origen_id", nullable = false)
    private Plaza origen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plaza_destino_id", nullable = false)
    private Plaza destino;
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoTrasladoPlaza estado = EstadoTrasladoPlaza.PROGRAMADO;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creado_por", nullable = false)
    private Trabajador creadoPor;
}
