package com.sigo.incidencia.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "incidencia_evidencias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncidenciaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incidencia_id", nullable = false)
    private Incidencia incidencia;

    @Column(name = "url_archivo", nullable = false, length = 500)
    private String urlArchivo;

    @Column(name = "public_id", length = 255)
    private String publicId;

    @Column(nullable = false, length = 20)
    private String tipo = "foto";

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
