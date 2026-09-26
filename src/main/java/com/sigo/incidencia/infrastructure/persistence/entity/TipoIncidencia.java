package com.sigo.incidencia.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_incidencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoIncidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo = true;
}
