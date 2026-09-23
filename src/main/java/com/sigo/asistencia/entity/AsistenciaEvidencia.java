package com.sigo.asistencia.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "asistencia_evidencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "asistencia_id",
            nullable = false
    )
    private AsistenciaRegistro asistencia;

    @Column(
            name = "url_archivo",
            nullable = false,
            length = 500
    )
    private String urlArchivo;

    /*
     * Identificador de Cloudinary.
     *
     * Nos permitirá eliminar realmente
     * la imagen de Cloudinary.
     *
     * Puede ser null para imágenes antiguas.
     */
    @Column(
            name = "public_id",
            length = 255
    )
    private String publicId;

    @Column(
            nullable = false,
            length = 20
    )
    private String tipo = "foto";

    @Column(
            name = "subido_en",
            insertable = false,
            updatable = false
    )
    private OffsetDateTime subidoEn;
}