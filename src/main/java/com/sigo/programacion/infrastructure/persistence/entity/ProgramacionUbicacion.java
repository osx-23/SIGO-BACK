package com.sigo.programacion.infrastructure.persistence.entity;

import com.sigo.personal.infrastructure.persistence.entity.Plaza;
import com.sigo.relevo.infrastructure.persistence.entity.Via;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="programacion_ubicacion", uniqueConstraints=@UniqueConstraint(columnNames={"plaza_id","codigo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProgramacionUbicacion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.EAGER, optional=false)
    @JoinColumn(name="plaza_id", nullable=false)
    private Plaza plaza;

    @Column(nullable=false, length=30)
    private String codigo;

    @Column(nullable=false, length=100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private TipoUbicacion tipo;

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="via_id")
    private Via via;

    @Column(nullable=false)
    private Boolean activo=true;

    @Column(nullable=false)
    private Integer orden=0;

    @Column(name="created_at", insertable=false, updatable=false)
    private OffsetDateTime createdAt;
}
